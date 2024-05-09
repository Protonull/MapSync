import { Socket } from "bun";
import { SyncClient } from "./sync-client.ts";
import { BufferReader } from "./buffer-reader.ts";

const MAX_PACKET_BYTES = 1 << 15;
const PACKET_LENGTH_BYTES = 4;
const MAX_PACKET_BODY_BYTES = MAX_PACKET_BYTES - PACKET_LENGTH_BYTES;

export class SyncSocket {
    #receivedBuffer: Buffer = Buffer.allocUnsafe(0);

    constructor(public readonly internal: Socket<SyncClient>) {}

    async handleReceivedData(raw: Buffer) {
        console.debug(
            `[${this.internal.data.name}] Received ${raw.byteLength} bytes!`,
        );

        this.#receivedBuffer = Buffer.concat([this.#receivedBuffer, raw]);
        const packetReader = new BufferReader(this.#receivedBuffer);

        while (true) {
            const readableBytes = packetReader.remainder;
            console.debug(
                `[${this.internal.data.name}] There are ${readableBytes} to parse!`,
            );

            if (readableBytes < PACKET_LENGTH_BYTES) {
                console.debug(
                    `[${this.internal.data.name}] There are only ${readableBytes} bytes, need to wait for at least ${PACKET_LENGTH_BYTES}!`,
                );
                break; // Await more data
            }

            const packetLength = packetReader.peekInt32();
            console.debug(
                `[${this.internal.data.name}] Packet declared its length as ${packetLength} bytes!`,
            );
            if (packetLength > MAX_PACKET_BODY_BYTES) {
                throw new Error(
                    `Packet length [${packetLength}] is too large! Max is [${MAX_PACKET_BODY_BYTES}]!`,
                );
            }

            const totalPacketLength = packetLength + PACKET_LENGTH_BYTES; // How many bytes does this packet take up (including prefixed length)
            if (readableBytes < totalPacketLength) {
                console.debug(
                    `[${this.internal.data.name}] There are only ${readableBytes} bytes, need to wait for ${totalPacketLength - readableBytes} more bytes!`,
                );
                break; // Await more data
            }

            // Skip past the packet length we peeked at earlier
            packetReader.skip(PACKET_LENGTH_BYTES);

            await this.internal.data.protocol.call(
                packetReader.pollInt8(),
                new BufferReader(packetReader.pollSlice(packetLength)),
            );

            if (packetReader.remainder == 0) {
                break;
            }
        }

        if (packetReader.position > 0 && packetReader.remainder > 0) {
            // Subarray is just a slice, but that shouldn't matter as the original Buffer should only remain in-memory
            // until .concat() is called on the next receive.
            this.#receivedBuffer = this.#receivedBuffer.subarray(
                packetReader.position,
            );
        }
    }
}
