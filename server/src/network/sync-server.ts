import { Socket } from "bun";
import { SyncClient } from "./sync-client.ts";
import { ensureError } from "../utilities/errors.ts";

export class SyncServer {
    readonly clients = new Map<bigint, SyncClient>();

    start(host: string, port: number) {
        const clients = new Map<bigint, SyncClient>();

        Bun.listen<SyncClient>({
            hostname: host,
            port: port,
            socket: {
                binaryType: "buffer",
                open: async (socket) => {
                    const client = (socket.data = new SyncClient(socket));
                    clients.set(client.id, client);
                    console.debug(`[${client.name}] Has connected!`);
                },
                close: async (socket) => {
                    const client = socket.data;
                    clients.delete(client.id);
                    console.debug(`[${client.name}] Has disconnected!`);
                },
                error: handleError,
                data: handleData,
            },
        });

        async function handleError(socket: Socket<SyncClient>, error: Error) {
            const client = socket.data;
            console.error(`[${client.name}] Has errored:`, error);
            socket.shutdown(false);
        }

        async function handleData(socket: Socket<SyncClient>, data: Buffer) {
            const client = socket.data;
            try {
                await client.socket.handleReceivedData(data);
            } catch (e) {
                await handleError(socket, ensureError(e));
            }
        }
    }
}
