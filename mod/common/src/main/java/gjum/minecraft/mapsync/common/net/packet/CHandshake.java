package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.net.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class CHandshake extends Packet {
	public static final int PACKET_ID = 1;

	public final @NotNull String modVersion;
	public final @NotNull String username;
	public final @NotNull String gameAddress;
	public final @NotNull String dimension;

	public CHandshake(@NotNull String modVersion, @NotNull String username, @NotNull String gameAddress, @NotNull String dimension) {
		this.modVersion = modVersion;
		this.username = username;
		this.gameAddress = gameAddress;
		this.dimension = dimension;
	}

	@Override
	public void write(ByteBuf out) {
		Packet.writeString(out, modVersion);
		Packet.writeString(out, username);
		Packet.writeString(out, gameAddress);
		Packet.writeString(out, dimension);
	}

	@Override
	public String toString() {
		return "CHandshake{" +
				"version='" + modVersion + '\'' +
				" username='" + username + '\'' +
				" gameAddress='" + gameAddress + '\'' +
				'}';
	}
}
