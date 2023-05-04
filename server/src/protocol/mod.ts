export enum Packets {
    "ERROR:pkt0" = 0,
    /**
     * The Minecraft client should send this packet IMMEDIATELY upon a
     * successful connection to the MapSync server.
     */
    "Handshake" = 1,
    /**
     * This is sent back to the client after the Handshake.
     */
    "EncryptionRequest" = 2,
    /**
     * Once this packet is received, the client should be considered fully
     * verified and thus can share map information.
     */
    "EncryptionResponse" = 3,
    /**
     * This is the first packet send to the client post-encryption setup. This
     * packet is used to inform the client when each region was last updated,
     * which the client can use to request newer regions from MapSync.
     */
    "RegionTimestamps" = 7,
    /**
     * This is a response to the RegionTimestampsPacket: the client is
     * requesting to be updated on regions that are outdated for it.
     */
    "RegionCatchup" = 8,
    /**
     * This is a clarification packet. It responds to the request with all the
     * regions' internal chunk timestamps. That way the client doesn't need to
     * receive 32x32 chunk's worth of data if only a single chunk inside is
     * newer to the client.
     */
    "Catchup" = 5,
    /**
     * This is the final packet in the catchup request: it contains a list of
     * all the chunks the client wishes to receive.
     */
    "CatchupRequest" = 6,
    /**
     * This is a bidirectional packet. It's used to fulfil chunk catchup
     * requests, but is also relayed verbatim to all other connected clients
     * when new chunk data is received.
     */
    "ChunkTile" = 4
}

/**
 * Attempts to retrieve a matching packet name for the given id.
 */
export function getPacketName(
    id: number
): string {
    const match = Packets[id] ?? null;
    if (match == null) {
        return "UNKNOWN:" +id;
    }
    return match;
}
