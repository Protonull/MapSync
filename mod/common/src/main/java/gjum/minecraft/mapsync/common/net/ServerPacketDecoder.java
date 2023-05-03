package gjum.minecraft.mapsync.common.net;

import gjum.minecraft.mapsync.common.net.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ServerPacketDecoder extends ReplayingDecoder<Void> {
	public static @Nullable IPacket constructServerPacket(int id, ByteBuf buf) {
		if (id == BID_ChunkDataPacket.PACKET_ID) return new BID_ChunkDataPacket(buf);
		if (id == S2C_EncryptionRequestPacket.PACKET_ID) return new S2C_EncryptionRequestPacket(buf);
		if (id == S2C_RegionCatchupResponsePacket.PACKET_ID) return S2C_RegionCatchupResponsePacket.read(buf);
		if (id == S2C_RegionTimestampsPacket.PACKET_ID) return new S2C_RegionTimestampsPacket(buf);
		return null;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
		try {
			byte id = buf.readByte();
			final IPacket packet = constructServerPacket(id, buf);
			if (packet == null) {
				SyncClient.logger.error("[ServerPacketDecoder] " +
						"Unknown server packet id " + id + " 0x" + Integer.toHexString(id));
				ctx.close();
				return;
			}
			out.add(packet);
		} catch (Throwable err) {
			err.printStackTrace();
			ctx.close();
		}
	}
}
