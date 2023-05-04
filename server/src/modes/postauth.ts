import node_util from "node:util";
import { BufReader, PacketBuilder } from "../deps/buffers";
import { UnsupportedPacketException } from "./mod";
import { TcpClient } from "../server";
import { Packets, getPacketName } from "../protocol/mod";
import * as database from "../database";
import { Pos2D } from "../protocol/structs";

type BufferHandler = (reader: BufReader) => Promise<void>;
type HandlerManager = Map<number, BufferHandler>;

export function set(
    client: TcpClient
) {
    const handlers: HandlerManager = new Map();

    handlers.set(
        Packets.ChunkTile,
        async (reader) => {
            const auth = client.requireAuth();
            const relayPacket = PacketBuilder.cast(reader.buffer);
            const dimension = reader.readString();
            if (dimension !== client.dimension) {
                client.warn("Client is attempting to send chunks for dimension [" + dimension + "] when they're currently in [" + client.dimension + "]");
                return;
            }
            await database.storeChunkData(
                dimension,
                reader.readInt32(), // Chunk X
                reader.readInt32(), // Chunk Z
                auth.uuid,
                reader.readUInt64(), // Timestamp
                reader.readUInt16(), // Chunk version
                reader.readBufWithLen(), // Chunk hash
                reader.readRemainder() // Chunk data
            );
            // TODO small timeout, then skip if other client already has it
            for (const otherClient of Object.values(client.server.clients)) {
                if (client === otherClient) continue;
                if (otherClient.getAuth() === false) continue;
                otherClient.send(relayPacket);
            }
        }
    );

    applyRegionCatchupHandler(handlers, client);

    client.handler = async function AuthenticatedPacketHandler(
        receivedPacketId: number,
        reader: BufReader
    ) {
        const handler = handlers.get(receivedPacketId) ?? null;
        if (handler === null) {
            throw new UnsupportedPacketException(`Received Packet[${getPacketName(receivedPacketId)}] and there's no handler for it!`);
        }
        await handler(reader);
    };
}

function applyRegionCatchupHandler(
    handlers: HandlerManager,
    client: TcpClient
) {
    handlers.set(Packets.RegionCatchup, async (reader) => {
        handlers.delete(Packets.RegionCatchup);

        const dimension = reader.readString();
        if (dimension !== client.dimension) {
            client.warn("Client is attempting to catch up dimension [" + dimension + "] when they're currently in [" + client.dimension + "]");
            return;
        }

        const regions = new Array<Pos2D>(reader.readInt16());
        for (let i = 0; i < regions.length; i++) {
            regions[i] = {
                x: reader.readInt16(),
                z: reader.readInt16()
            };
        }

        const chunks = await database.getChunkTimestamps(dimension, regions);
        if (chunks.length < 1) {
            return;
        }

        client.send(
            PacketBuilder
                .packet(Packets.Catchup)
                .writeString(dimension)
                .writeMany((builder) => {
                    builder.writeUInt32(chunks.length);
                    for (const region of chunks) {
                        builder.writeInt32(region.x);
                        builder.writeInt32(region.z);
                        builder.writeUInt64(region.timestamp);
                    }
                })
        );

        applyCatchupRequestHandler(handlers, client);
    });
}

function applyCatchupRequestHandler(
    handlers: HandlerManager,
    client: TcpClient
) {
    handlers.set(Packets.CatchupRequest, async (reader) => {
        handlers.delete(Packets.CatchupRequest);

        const dimension = reader.readString();
        if (dimension !== client.dimension) {
            client.warn("Client is attempting to catch up dimension [" + dimension + "] when they're currently in [" + client.dimension + "]");
            return;
        }

        for (let i = 0, l = reader.readUInt32(); i < l; i++) {
            const x = reader.readInt32();
            const z = reader.readInt32();
            const timestamp = reader.readUInt64();
            const chunk = await database.getChunkData(
                dimension,
                x,
                z,
                timestamp
            );
            if (!chunk) {
                client.warn(`Requested unavailable chunk! [${node_util.inspect({ dimension, x, z, timestamp })}]`);
                continue;
            }
            client.send(
                PacketBuilder
                    .packet(Packets.ChunkTile)
                    .writeString(dimension)
                    .writeInt32(x)
                    .writeInt32(z)
                    .writeUInt64(timestamp)
                    .writeUInt16(chunk.version)
                    .writeBufWithLen(chunk.hash)
                    .writeBufRaw(chunk.data) // XXX do we need to prefix with length?
            );
        }
    });
}
