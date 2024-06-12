package gjum.minecraft.mapsync.mod.network;

import gjum.minecraft.mapsync.mod.network.packet.ChunkTilePacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundCatchupRequestPacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundChunkTimestampsRequestPacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundEncryptionResponsePacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundHandshakePacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ServerboundPacketEncoder extends MessageToByteEncoder<Packet> {
	public static int getClientPacketId(Packet packet) {
		if (packet instanceof ChunkTilePacket) return ChunkTilePacket.PACKET_ID;
		if (packet instanceof ServerboundHandshakePacket) return ServerboundHandshakePacket.PACKET_ID;
		if (packet instanceof ServerboundEncryptionResponsePacket) return ServerboundEncryptionResponsePacket.PACKET_ID;
		if (packet instanceof ServerboundCatchupRequestPacket) return ServerboundCatchupRequestPacket.PACKET_ID;
		if (packet instanceof ServerboundChunkTimestampsRequestPacket) return ServerboundChunkTimestampsRequestPacket.PACKET_ID;
		throw new IllegalArgumentException("Unknown client packet class " + packet);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
		try {
			out.writeByte(getClientPacketId(packet));
			packet.write(out);
		} catch (Throwable err) {
			err.printStackTrace();
			ctx.close();
		}
	}
}
