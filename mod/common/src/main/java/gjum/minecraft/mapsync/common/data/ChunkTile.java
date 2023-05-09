package gjum.minecraft.mapsync.common.data;

import gjum.minecraft.mapsync.common.net.packet.IPacket;
import gjum.minecraft.mapsync.common.utilities.SHA1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record ChunkTile(
		@NotNull ResourceKey<Level> dimension,
		int x,
		int z,
		long timestamp,
		int dataVersion,
		BlockColumn @NotNull [] columns
) {
	public ChunkPos chunkPos() {
		return new ChunkPos(x, z);
	}

	public byte @NotNull [] generateHash() {
		return SHA1.hash((digest) -> {
			final ByteBuf columnsBuf = Unpooled.buffer();
			ChunkTile.writeColumns(columns(), columnsBuf);
			digest.update(columnsBuf.nioBuffer());
		});
	}

	public void write(ByteBuf buf) {
		writeMetadata(buf);
		writeColumns(columns, buf);
	}

	/**
	 * without columns
	 */
	public void writeMetadata(ByteBuf buf) {
		IPacket.writeString(buf, dimension.location().toString());
		buf.writeInt(x);
		buf.writeInt(z);
		buf.writeLong(timestamp);
		buf.writeShort(dataVersion);
		buf.writeInt(0);
	}

	public static void writeColumns(BlockColumn[] columns, ByteBuf buf) {
		// TODO compress
		for (BlockColumn column : columns) {
			column.write(buf);
		}
	}

	public static ChunkTile fromBuf(ByteBuf buf) {
		String dimensionStr = IPacket.readString(buf);
		var dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimensionStr));
		int x = buf.readInt();
		int z = buf.readInt();
		long timestamp = buf.readLong();
		int dataVersion = buf.readUnsignedShort();
		buf.skipBytes(buf.readInt()); // Skip hash
		var columns = new BlockColumn[256];
		for (int i = 0; i < 256; i++) {
			columns[i] = BlockColumn.fromBuf(buf);
		}
		return new ChunkTile(dimension, x, z, timestamp, dataVersion, columns);
	}
}
