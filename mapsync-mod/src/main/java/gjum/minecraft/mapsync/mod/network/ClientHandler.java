package gjum.minecraft.mapsync.mod.network;

import gjum.minecraft.mapsync.mod.MapSyncMod;
import gjum.minecraft.mapsync.mod.data.CatchupChunk;
import gjum.minecraft.mapsync.mod.network.packet.ChunkTilePacket;
import gjum.minecraft.mapsync.mod.network.packet.ClientboundChunkTimestampsResponsePacket;
import gjum.minecraft.mapsync.mod.network.packet.ClientboundEncryptionRequestPacket;
import gjum.minecraft.mapsync.mod.network.packet.ClientboundRegionTimestampsPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.net.ConnectException;

/**
 * tightly coupled to {@link SyncClient}
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
	private final SyncClient client;

	public ClientHandler(SyncClient client) {
		this.client = client;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object packet) {
		try {
			if (!client.isEncrypted()) {
				if (packet instanceof ClientboundEncryptionRequestPacket pktEncryptionRequest) {
					client.setUpEncryption(ctx, pktEncryptionRequest);
				} else throw new Error("Expected encryption request, got " + packet);
			} else if (packet instanceof ChunkTilePacket pktChunkTile) {
				MapSyncMod.getMod().handleSharedChunk(pktChunkTile.chunkTile);
			} else if (packet instanceof ClientboundRegionTimestampsPacket pktRegionTimestamps) {
				MapSyncMod.getMod().handleRegionTimestamps(pktRegionTimestamps, client);
			} else if (packet instanceof ClientboundChunkTimestampsResponsePacket pktCatchup) {
				for (CatchupChunk chunk : pktCatchup.chunks) {
					chunk.syncClient = this.client;
				}
				MapSyncMod.getMod().handleCatchupData((ClientboundChunkTimestampsResponsePacket) packet);
			} else throw new Error("Expected packet, got " + packet);
		} catch (Throwable err) {
			err.printStackTrace();
			ctx.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable err) throws Exception {
		if (err instanceof IOException && "Connection reset by peer".equals(err.getMessage())) return;
		if (err instanceof ConnectException && err.getMessage().startsWith("Connection refused: ")) return;

		SyncClient.logger.info("[map-sync] Network Error: " + err);
		err.printStackTrace();
		ctx.close();
		super.exceptionCaught(ctx, err);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		client.handleDisconnect(new RuntimeException("Channel inactive"));
		super.channelInactive(ctx);
	}
}
