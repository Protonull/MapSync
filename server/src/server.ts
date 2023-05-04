import node_crypto from "node:crypto";
import node_net from "node:net";
import node_util from "node:util";
import { PacketHandler } from "./modes/mod";
import { Mutex } from "async-mutex";
import { Packets, getPacketName } from "./protocol/mod";
import { BufReader, duplicateSlice, PacketBuilder } from "./deps/buffers";
import * as metadata from "./metadata";
import * as preAuthMode from "./modes/preauth";

const { PORT = "12312", HOST = "127.0.0.1" } = process.env;

const FRAME_SIZE_BYTES = 4;
const MAX_FRAME_SIZE = 1 << 24;

export class TcpServer {
    public readonly server: node_net.Server;
    public readonly clients = new Map<bigint, TcpClient>();

    private readonly KEY_PAIR = node_crypto.generateKeyPairSync("rsa", {
        modulusLength: 1024
    });
    public readonly PUBLIC_KEY_BUFFER = this.KEY_PAIR.publicKey.export({
        type: "spki",
        format: "der"
    });

    public constructor(
        public readonly config: metadata.Config
    ) {
        this.server = node_net.createServer({}, (socket) => {
            const client = new TcpClient(socket, this);
            this.clients.set(client.id, client);
            client.log("Connected from", socket.remoteAddress);

            socket.on("end", () => {
                // This event is called when the other end signals the end of
                // transmission, meaning this client is still writeable but not
                // readable. In this situation we just want to close the socket.
                // https://nodejs.org/dist/latest-v18.x/docs/api/net.html#event-end
                client.kick("Ended");
            });
            socket.on("timeout", () => {
                // As per the docs, the socket needs to be manually closed
                // https://nodejs.org/dist/latest-v18.x/docs/api/net.html#event-timeout
                client.kick("Timed out");
            });
            socket.on("error", (err) => {
                client.kick("Socket error: " + node_util.inspect(err));
            });
            socket.on("close", (hadError) => {
                client._auth = undefined;
                this.clients.delete(client.id);
                receivingMutex.cancel();
                client.log("Closed.", { hadError });
            });

            const receivingMutex = new Mutex();
            let receivedBuffer: Buffer = Buffer.alloc(0);
            socket.on("data", (data: Buffer) => {
                receivingMutex.runExclusive(async () => {
                    try {
                        data = await client._auth?.decipher.update(data) ?? data;
                    }
                    catch (err) {
                        client.kick("Could not decipher received data: " + node_util.inspect(err));
                        return;
                    }

                    const combinedReceivedSize = receivedBuffer.length + data.length;
                    if (combinedReceivedSize > MAX_FRAME_SIZE) {
                        client.kick("They're sending too much data! Frame reached size of [" + combinedReceivedSize + "] when the max is [" + MAX_FRAME_SIZE + "]");
                        return;
                    }

                    receivedBuffer = Buffer.concat([ receivedBuffer, data ]);
                    let lastOffset = 0, remainingLength: number;

                    while ((remainingLength = receivedBuffer.length - lastOffset) > FRAME_SIZE_BYTES) {
                        const packetLength = receivedBuffer.readUInt32BE(lastOffset);
                        if (packetLength > MAX_FRAME_SIZE) {
                            client.kick("The packets they're sending are too large! Packet length specified as [" + packetLength + "] when the max is [" + MAX_FRAME_SIZE + "]");
                            return;
                        }
                        if ((packetLength + FRAME_SIZE_BYTES) > remainingLength) {
                            // Need to wait until more data has arrived
                            break;
                        }
                        lastOffset += FRAME_SIZE_BYTES; // Wait till here to increase the offset to allow for graceful break-and-remainder-copy
                        await client.handlePacketReceived(new BufReader(
                            duplicateSlice(receivedBuffer, lastOffset, packetLength)
                        ));
                        lastOffset += packetLength;
                    }

                    receivedBuffer = duplicateSlice(receivedBuffer, lastOffset, remainingLength);
                });
            });
        });

        this.server.on("error", (err: Error) => {
            console.error("[TcpServer] Error:", err);
            this.server.close();
        });

        this.server.listen({ port: PORT, hostname: HOST }, () => {
            console.log("[TcpServer] Listening on", HOST, PORT);
        });
    }

    public decrypt(buffer: Buffer): Buffer {
        return node_crypto.privateDecrypt(
            {
                key: this.KEY_PAIR.privateKey,
                padding: node_crypto.constants.RSA_PKCS1_PADDING
            },
            buffer
        );
    }
}

let nextClientId = 1n;

/** Prefixes packets with their length (UInt32BE);
 * handles Mojang authentication */
export class TcpClient {
    readonly id = nextClientId++;
    /** contains mojang name once logged in */
    public name = "Client" + this.id;

    public handler: PacketHandler = null!;

    public _auth?: Readonly<{
        uuid: string
        username: string
        encipher: node_crypto.Cipher
        decipher: node_crypto.Decipher
    }>;
    public dimension?: string;

    public constructor(
        private readonly socket: node_net.Socket,
        public readonly server: TcpServer
    ) {
        preAuthMode.setStage1(this);
    }

    public getAuth() {
        return this._auth ?? false;
    }

    public requireAuth() {
        const auth = this.getAuth();
        if (auth === false) {
            throw new Error(`Client[${this.name}] not authenticated`);
        }
        return auth;
    }

    public kick(
        internalReason: string
    ) {
        this.log("Kicking: " + internalReason);
        this.socket.destroy();
    }

    public async send(
        packet: PacketBuilder,
        doCrypto = true
    ) {
        if (doCrypto) this.requireAuth();
        this.debug(`MapSync[${getPacketName(packet.packet)}] → ${this.name}`);
        let buffer = packet.getBuffer();
        if (doCrypto) buffer = await this._auth!.encipher.update(buffer);
        this.socket.write(buffer);
    }

    public async handlePacketReceived(
        reader: BufReader
    ) {
        try {
            const packetId = reader.readUInt8(),
                packetType = Packets[packetId];
            if (packetType === undefined) {
                // noinspection ExceptionCaughtLocallyJS
                throw new Error(
                    "Unknown server←client packet [" +
                    packetId +
                    ":" +
                    reader.readRemainder().toString("base64") +
                    "]"
                );
            }
            this.debug(`MapSync ← ${this.name}[${packetType}]`);
            await this.handler(packetId, reader);
        }
        catch (err) {
            this.warn(err);
            this.kick("Something errored server side while packet handling!");
            return;
        }
    }

    public debug(...args: any[]) {
        if (process.env.NODE_ENV === "production") return;
        console.debug(`[${this.name}]`, ...args);
    }

    public log(...args: any[]) {
        console.log(`[${this.name}]`, ...args);
    }

    public warn(...args: any[]) {
        console.error(`[${this.name}]`, ...args);
    }
}
