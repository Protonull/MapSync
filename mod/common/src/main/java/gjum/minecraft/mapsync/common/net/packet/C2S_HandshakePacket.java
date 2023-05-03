package gjum.minecraft.mapsync.common.net.packet;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * The Minecraft client should send this packet IMMEDIATELY upon a successful
 * connection to the MapSync server.
 *
 * @param modVersion The MapSync version (effectively the protocol version)
 * @param username The client's Mojang username (not their email)
 * @param gameAddress The server-address for the Minecraft server they're connected to.
 * @param dimension The dimension the client is in and wish to sync.
 */
public record C2S_HandshakePacket(
		@NotNull String modVersion,
		@NotNull String username,
		@NotNull String gameAddress,
		@NotNull String dimension
) implements IPacket {
	public static final int PACKET_ID = 1;

	@Override
	public void write(final @NotNull ByteBuf buffer) {
		IPacket.writeString(buffer, this.modVersion);
		IPacket.writeString(buffer, this.username);
		IPacket.writeString(buffer, this.gameAddress);
		IPacket.writeString(buffer, this.dimension);
	}
}
