import "./cli";
import * as database from "./database";
import * as metadata from "./metadata";
import { ClientPacket } from "./protocol";
import { CatchupRequestPacket } from "./protocol/CatchupRequestPacket";
import { ChunkTilePacket } from "./protocol/ChunkTilePacket";
import { TcpClient, TcpServer } from "./server";
import { RegionCatchupPacket } from "./protocol/RegionCatchupPacket";

let config: metadata.Config = null!;
Promise.resolve().then(async () => {
    await database.setup();

    config = await metadata.getConfig();

    // These two are only used if whitelist is enabled... but best to load them
    // anyway lest there be a modification to them that is then saved.
    await metadata.whitelist_load();
    await metadata.uuid_cache_load();

    new Main();
});

type ProtocolClient = TcpClient; // TODO cleanup

export class Main {
    server = new TcpServer(this);

    //Cannot be async, as it's caled from a synchronous constructor
    handleClientConnected(client: ProtocolClient) {}

    async handleClientAuthenticated(client: ProtocolClient) {
        const auth = client.requireAuth();

        metadata.uuid_cache.set(auth.username!, auth.uuid);
        await metadata.uuid_cache_save();

        if (config.whitelist) {
            if (!metadata.whitelist.has(auth.uuid)) {
                client.log(
                    `Rejected unwhitelisted user ${auth.username} (${auth.uuid})`
                );
                client.kick(`Not whitelisted`);
                return;
            }
        }

        // TODO check version, mc server, user access

        const timestamps = await database.getRegionTimestamps(client.dimension!);
        client.send({
            type: "RegionTimestamps",
            dimension: client.dimension!,
            regions: timestamps
        });
    }

    handleClientDisconnected(client: ProtocolClient) {}

    handleClientPacketReceived(client: ProtocolClient, pkt: ClientPacket) {
        switch (pkt.type) {
            case "ChunkTile":
                return this.handleChunkTilePacket(client, pkt);
            case "CatchupRequest":
                return this.handleCatchupRequest(client, pkt);
            case "RegionCatchup":
                return this.handleRegionCatchupPacket(client, pkt);
            default:
                throw new Error(
                    `Unknown packet '${(pkt as any).type}' from client ${
                        client.id
                    }`
                );
        }
    }

    async handleChunkTilePacket(client: ProtocolClient, pkt: ChunkTilePacket) {
        const auth = client.requireAuth();

        // TODO ignore if same chunk hash exists in db

        await database.storeChunkData(
            pkt.dimension,
            pkt.chunk_x,
            pkt.chunk_z,
            auth.uuid,
            pkt.ts,
            pkt.data.version,
            pkt.data.hash,
            pkt.data.data
        );

        // TODO small timeout, then skip if other client already has it
        for (const otherClient of Object.values(this.server.clients)) {
            if (client === otherClient) continue;
            if (otherClient.getAuth() === false) continue;
            otherClient.send(pkt);
        }

        // TODO queue tile render for web map
    }

    async handleCatchupRequest(
        client: ProtocolClient,
        pkt: CatchupRequestPacket
    ) {
        client.requireAuth();

        for (const req of pkt.chunks) {
            const { dimension, chunk_x, chunk_z } = req;

            let chunk = await database.getChunkData(
                dimension,
                chunk_x,
                chunk_z,
                req.ts
            );

            if (!chunk) {
                console.error(
                    `${client.name} requested unavailable chunk`,
                    req
                );
                continue;
            }

            client.send({
                type: "ChunkTile",
                dimension,
                chunk_x,
                chunk_z,
                ts: req.ts,
                data: chunk
            });
        }
    }

    async handleRegionCatchupPacket(
        client: ProtocolClient,
        pkt: RegionCatchupPacket
    ) {
        client.requireAuth();

        const chunks = await database.getChunkTimestamps(
            pkt.dimension,
            pkt.regions
        );
        if (chunks.length) client.send({
            type: "Catchup",
            chunks: chunks.map((chunk) => ({
                dimension: chunk.dimension,
                chunk_x: chunk.x,
                chunk_z: chunk.z,
                ts: chunk.timestamp
            }))
        });
    }
}
