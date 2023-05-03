package gjum.minecraft.mapsync.common.net;

import gjum.minecraft.mapsync.common.net.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ClientPacketEncoder extends MessageToByteEncoder<IPacket> {
	public static int getClientPacketId(IPacket packet) {
		if (packet instanceof BID_ChunkDataPacket) return BID_ChunkDataPacket.PACKET_ID;
		if (packet instanceof C2S_HandshakePacket) return C2S_HandshakePacket.PACKET_ID;
		if (packet instanceof C2S_EncryptionResponsePacket) return C2S_EncryptionResponsePacket.PACKET_ID;
		if (packet instanceof C2S_ChunkCatchupRequestPacket) return C2S_ChunkCatchupRequestPacket.PACKET_ID;
		if (packet instanceof C2S_RegionCatchupRequestPacket) return C2S_RegionCatchupRequestPacket.PACKET_ID;
		throw new IllegalArgumentException("Unknown client packet class " + packet);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, IPacket packet, ByteBuf out) {
		try {
			out.writeByte(getClientPacketId(packet));
			packet.write(out);
		} catch (Throwable err) {
			err.printStackTrace();
			ctx.close();
		}
	}
}
