import { BufReader } from "../deps/buffers";
import { Packets, getPacketName } from "../protocol/mod";

export type PacketHandler = (packetId: number, reader: BufReader) => Promise<void>;

/**
 * Convenience wrapper for packet handlers that only expect one packet type.
 */
export function singularPacketHandler(
    expectedPacketId: Packets,
    singularHandler: (reader: BufReader) => Promise<void>
): PacketHandler {
    return async function SingularPacketHandler(
        receivedPacketId,
        reader
    ) {
        if (receivedPacketId !== expectedPacketId) {
            throw new UnsupportedPacketException(`Received Packet[${getPacketName(receivedPacketId)}] while expecting Packet[${getPacketName(expectedPacketId)}]`);
        }
        await singularHandler(reader);
    }
}

/**
 * Throw this at the bottom of your AbstractClientMode.onPacketReceived if the
 * packet is unsupported.
 */
export class UnsupportedPacketException extends Error {

}
