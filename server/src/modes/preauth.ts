import node_crypto from "node:crypto";
import fetch from "node-fetch";
import { PacketBuilder } from "../deps/buffers";
import * as errors from "../deps/errors";
import { singularPacketHandler } from "./mod";
import { TcpClient } from "../server";
import { Packets } from "../protocol/mod";
import * as metadata from "../metadata";
import { getRegionTimestamps } from "../database";
import * as postAuthMode from "./postauth";

export function setStage1(
    client: TcpClient
) {
    client.handler = singularPacketHandler(Packets.Handshake, async function PreAuthStage1Handler(reader) {
        const modVersion = reader.readString(); // The MapSync version (effectively the protocol version)
        const mojangName = reader.readString(); // The client's Mojang username (not their email)
        const gameAddress = reader.readString(); // The server-address for the Minecraft server they're connected to.
        const dimension = reader.readString(); // The dimension the client is in.

        client.dimension = dimension;

        const verifyToken = node_crypto.randomBytes(4);
        client.send(
            PacketBuilder
                .packet(Packets.EncryptionRequest)
                .writeBufWithLen(client.server.PUBLIC_KEY_BUFFER)
                .writeBufWithLen(verifyToken),
            false
        );
        setStage2(
            client,
            mojangName,
            verifyToken,
            dimension
        );
    });
}

export function setStage2(
    client: TcpClient,
    claimedUsername: string,
    expectedVerifyToken: Buffer,
    dimension: string
) {
    function createAuth(
        uuid: string,
        name: string,
        secret: Buffer
    ) {
        return {
            uuid,
            username: name,
            encipher: node_crypto.createCipheriv("aes-128-cfb8", secret, secret),
            decipher: node_crypto.createDecipheriv("aes-128-cfb8", secret, secret)
        };
    }

    client.handler = singularPacketHandler(Packets.EncryptionResponse, async function PreAuthStage2Handler(reader) {
        // This packet is awkward in that it sends the secret first but handles the verify-token first. ಠ_ಠ
        const receivedSharedSecret = await client.server.decrypt(reader.readBufWithLen());
        const receivedVerifyToken = await client.server.decrypt(reader.readBufWithLen());

        if (!receivedVerifyToken.equals(expectedVerifyToken)) {
            client.kick(`Incorrect verifyToken! Expected [${expectedVerifyToken}], got [${receivedVerifyToken}]`);
            return;
        }
        if (process.env["DISABLE_AUTH"] === "true") {
            client._auth = createAuth(
                "AUTH-DISABLED-" + claimedUsername,
                claimedUsername,
                receivedSharedSecret
            );
        }
        else {
            const auth = await fetchHasJoined(
                claimedUsername,
                node_crypto
                    .createHash("sha1")
                    .update(receivedSharedSecret)
                    .update(client.server.PUBLIC_KEY_BUFFER)
                    .digest()
                    .toString("hex")
            );
            if (auth instanceof Error) {
                client.kick(`Mojang auth failed`);
                client.warn(auth);
                return;
            }
            client.log("Authenticated as", auth);
            client._auth = createAuth(
                auth.uuid,
                auth.name,
                receivedSharedSecret
            );
            metadata.uuid_cache.set(auth.name, auth.uuid);
            await metadata.uuid_cache_save();
            if (client.server.config.whitelist && !metadata.whitelist.has(auth.uuid)) {
                client.kick("Rejecting unwhitelisted user!");
                return;
            }
        }
        client.name += ":" + client._auth.username;
        const regions = await getRegionTimestamps(dimension);
        if (regions.length > 32_767) {
            // TODO: Remove this if it's not an issue
            console.error("Attempting to send region timestamps, but the regions surpass the maximum value for a signed-short length!");
        }
        client.send(
            PacketBuilder
                .packet(Packets.RegionTimestamps)
                .writeString(dimension)
                .writeMany((builder) => {
                    builder.writeInt16(regions.length);
                    for (const region of regions) {
                        builder.writeInt16(region.x);
                        builder.writeInt16(region.z);
                        builder.writeInt64(region.timestamp);
                    }
                })
        );
        postAuthMode.set(client);
    });
}

function fetchHasJoined(
    username: string,
    shaHex: string
) {
    return fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${username}&serverId=${shaHex}`)
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
