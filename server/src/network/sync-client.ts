import { Socket } from "bun";
import { SyncSocket } from "./sync-socket.ts";
import { PacketProtocol } from "./protocol.ts";

let lastClientId: bigint = 1n;
export class SyncClient {
    readonly id: bigint = ++lastClientId;
    readonly socket: SyncSocket;
    readonly address: string;
    readonly protocol = new PacketProtocol(this);

    constructor(socket: Socket<SyncClient>) {
        this.socket = new SyncSocket(socket);
        this.address = `${this.socket.internal.remoteAddress}:${this.socket.internal.localPort}`;
    }

    get name(): string {
        return this.address;
    }

    kick(reason: string) {
        console.warn(`[${this.name}] Kicked: ${reason}`);
        this.socket.internal.shutdown(false);
    }
}
