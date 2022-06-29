import node_crypto from "node:crypto";
import node_net from "node:net";
import node_util from "node:util";
import * as errors from "./deps/errors";
import { Main } from "./main";
import { Mutex } from "async-mutex";
import fetch from "node-fetch";
import * as protocol from "./protocol";
import { BufReader, BufWriter, duplicateSlice } from "./deps/buffers";
import { EncryptionResponsePacket } from "./protocol/EncryptionResponsePacket";
import { HandshakePacket } from "./protocol/HandshakePacket";

const { PORT = "12312", HOST = "127.0.0.1" } = process.env;

type ProtocolHandler = Main; // TODO cleanup

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
        private readonly handler: ProtocolHandler
    ) {
        this.server = node_net.createServer({}, (socket) => {
            const client = new TcpClient(socket, this, handler);
            this.clients.set(client.id, client);
            client.log("Connected from", socket.remoteAddress);
            handler.handleClientConnected(client);

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
                        try {
                            const packetBuffer = duplicateSlice(receivedBuffer, lastOffset, packetLength);
                            const packerReader = new BufReader(packetBuffer);
                            const packet = protocol.decodePacket(packerReader);
                            await client.handlePacketReceived(packet);
                        }
                        catch (e) {
                            client.warn(e);
                            client.kick("Error in packet handler");
                            return;
                        }
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

    public _auth?: Readonly<{
        uuid: string
        username: string
        encipher: node_crypto.Cipher
        decipher: node_crypto.Decipher
    }>;
    public dimension?: string;

    /** sent by client during handshake */
    private claimedMojangName?: string;
    private verifyToken?: Buffer;

    public constructor(
        private readonly socket: node_net.Socket,
        private readonly server: TcpServer,
        private readonly handler: ProtocolHandler
    ) { }

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
        packet: protocol.ServerPacket,
        doCrypto = true
    ) {
        if (doCrypto) this.requireAuth();
        this.debug(`MapSync[${packet.type}] → ${this.name}`);
        const writer = new BufWriter(); // TODO size hint
        writer.writeUInt32(0); // set later, but reserve space in buffer
        protocol.encodePacket(packet, writer);
        let buffer = writer.getBuffer();
        buffer.writeUInt32BE(buffer.length - 4, 0); // write into space reserved above
        if (doCrypto) buffer = await this._auth!.encipher.update(buffer);
        this.socket.write(buffer);
    }

    public async handlePacketReceived(
        packet: protocol.ClientPacket
    ) {
        this.debug(`MapSync ← ${this.name}[${packet.type}]`);
        if (this.getAuth() === false) { // not authenticated yet
            // not authenticated yet
            switch (packet.type) {
                case "Handshake":
                    return await this.handleHandshakePacket(packet);
                case "EncryptionResponse":
                    return await this.handleEncryptionResponsePacket(packet);
            }
            throw new Error(
                `Packet ${packet.type} from unauth'd client ${this.id}`
            );
        }
        else {
            return await this.handler.handleClientPacketReceived(this, packet);
        }
    }

    private async handleHandshakePacket(packet: HandshakePacket) {
        if (this.getAuth() !== false) throw new Error(`Already authenticated`);
        if (this.verifyToken) throw new Error(`Encryption already started`);
        // TODO: Check "packet.modVersion" here
        // TODO: Check "packet.gameAddress" here
        this.claimedMojangName = packet.mojangName;
        this.dimension = packet.dimension;
        this.verifyToken = node_crypto.randomBytes(4);
        await this.send({
            type: "EncryptionRequest",
            publicKey: this.server.PUBLIC_KEY_BUFFER,
            verifyToken: this.verifyToken
        }, false);
    }

    private async handleEncryptionResponsePacket(
        pkt: EncryptionResponsePacket
    ) {
        if (this.getAuth() !== false) {
            throw new Error(`Already authenticated`);
        }
        if (!this.claimedMojangName) {
            throw new Error(`Encryption has not started: no mojangName`);
        }
        if (!this.verifyToken) {
            throw new Error(`Encryption has not started: no verifyToken`);
        }

        const verifyToken = this.server.decrypt(pkt.verifyToken);
        if (!this.verifyToken.equals(verifyToken)) {
            this.kick(
                `Incorrect verifyToken! Expected [${this.verifyToken}], got [${verifyToken}]`
            );
            return;
        }

        const secret = this.server.decrypt(pkt.sharedSecret);
        const shaHex = node_crypto
            .createHash("sha1")
            .update(secret)
            .update(this.server.PUBLIC_KEY_BUFFER)
            .digest()
            .toString("hex");

        const auth = await fetchHasJoined(this.claimedMojangName, shaHex);
        if (auth instanceof Error) {
            this.kick("Mojang auth failed");
            this.warn(auth);
            return;
        }

        this.log("Authenticated as " + auth.name);
        this.name += ":" + auth.name;
        this._auth = {
            uuid: auth.uuid,
            username: auth.name,
            encipher: node_crypto.createCipheriv(
                "aes-128-cfb8",
                secret,
                secret
            ),
            decipher: node_crypto.createDecipheriv(
                "aes-128-cfb8",
                secret,
                secret
            )
        };

        await this.handler.handleClientAuthenticated(this);
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

async function fetchHasJoined(
    username: string,
    shaHex: string
) {
    // if auth is disabled, return a "usable" item
    if (process.env["DISABLE_AUTH"] === "true")
        return { name: username, uuid: `AUTH-DISABLED-${username}` }

    return await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${username}&serverId=${shaHex}`)
        .then((res) => {
            if (res.status === 204) {
                throw new Error("Auth returned 204 (empty)");
            }
            return res.json();
        })
        .then((res: { name: string; id: string; }) => ({
            name: res.name,
            uuid: res.id.replace(
                /^(........)-?(....)-?(....)-?(....)-?(............)$/,
                "$1-$2-$3-$4-$5"
            )
        }))
        .catch((err) => errors.ensureError(err));
}
