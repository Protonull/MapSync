package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.data.CatchupChunk;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * @param chunks Chunks must all be in the same dimension
 */
public record C2S_ChunkCatchupRequestPacket(
		@NotNull List<@NotNull CatchupChunk> chunks
) implements IPacket {
	public static final int PACKET_ID = 6;

	public C2S_ChunkCatchupRequestPacket {
		if (chunks.isEmpty()) {
			throw new Error("Chunks list must not be empty");
		}
		ResourceKey<Level> dimension = null;
		for (final CatchupChunk chunk : chunks) {
			if (dimension == null) {
				dimension = chunk.dimension();
			}
			else if (!dimension.equals(chunk.dimension())) {
				throw new Error("Chunks must all be in the same dimension " + dimension + " but this one was " + chunk.dimension());
			}
		}
		chunks = List.copyOf(chunks);
	}

	@Override
	public void write(final @NotNull ByteBuf buffer) {
		IPacket.writeString(buffer, this.chunks.get(0).dimension().location().toString());
		buffer.writeInt(this.chunks.size());
		for (final CatchupChunk chunk : this.chunks) {
			buffer.writeInt(chunk.chunk_x());
			buffer.writeInt(chunk.chunk_z());
			buffer.writeLong(chunk.timestamp());
		}
	}
}
