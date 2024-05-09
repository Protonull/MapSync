import { SyncClient } from "./sync-client.ts";
import { BufferReader } from "./buffer-reader.ts";

export type PacketHandler = (
    packetReader: BufferReader,
) => Promise<void> | void;

export class PacketProtocol {
    readonly #client: SyncClient;
    readonly handlers = new Map<number, PacketHandler>();

    constructor(client: SyncClient) {
        this.#client = client;
    }

    async call(packetId: number, packetReader: BufferReader) {
        const handler = this.handlers.get(packetId) ?? null;
        if (handler === null) {
            this.#client.kick(
                `Unexpected packet id [${packetId}]! Only expecting [${[...this.handlers.keys()]}]!`,
            );
            return;
        }
        await handler(packetReader);
    }
}
