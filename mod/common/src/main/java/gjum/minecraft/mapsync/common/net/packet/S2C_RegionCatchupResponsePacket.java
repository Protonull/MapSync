package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.data.CatchupChunk;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * This packet clarifies returns the internal chunk timestamps for all the
 * regions the client requested. The chunks <i>should</i> be ordered newest to
 * oldest.
 */
public record S2C_RegionCatchupResponsePacket(
		@NotNull List<@NotNull CatchupChunk> chunks
) implements IPacket {
	public static final int PACKET_ID = 5;

	public S2C_RegionCatchupResponsePacket {
		chunks = List.copyOf(chunks);
	}

	/**
	 * Have to keep this static method in lieu of a constructor because of the
	 * dimension shenanigans going on here. Keep an eye on
	 * <a href="https://openjdk.org/jeps/8300786">JEP Draft 8300786</a>.
	 */
	public static S2C_RegionCatchupResponsePacket read(final @NotNull ByteBuf buffer) {
		final ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(
				IPacket.readString(buffer)
		));
		final var chunks = new CatchupChunk[buffer.readInt()];
		for (int i = 0; i < chunks.length; i++) {
			chunks[i] = new CatchupChunk(
					dimension,
					buffer.readInt(),
					buffer.readInt(),
					buffer.readLong()
			);
		}
		return new S2C_RegionCatchupResponsePacket(List.of(chunks));
	}
}
