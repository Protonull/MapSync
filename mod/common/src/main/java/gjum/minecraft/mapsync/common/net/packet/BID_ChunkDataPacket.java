package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.data.ChunkTile;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public record BID_ChunkDataPacket(
		@NotNull ChunkTile chunkTile
) implements IPacket {
	public static final int PACKET_ID = 4;

	public BID_ChunkDataPacket(final @NotNull ByteBuf buffer) {
		this(
				ChunkTile.fromBuf(buffer)
		);
	}

	@Override
	public void write(final @NotNull ByteBuf buffer) {
		this.chunkTile.write(buffer);
	}
}
