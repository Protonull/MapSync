package gjum.minecraft.mapsync.common.data;

import gjum.minecraft.mapsync.common.net.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record ChunkTile(
		ResourceKey<Level> dimension,
		int x, int z,
		long timestamp,
		int dataVersion,
		byte[] dataHash,
		BlockColumn[] columns
) {
	public ChunkPos chunkPos() {
		return new ChunkPos(x, z);
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
		buf.writeInt(dataHash.length); // TODO could be Short as hash length is known to be small
		buf.writeBytes(dataHash);
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
		byte[] hash = new byte[buf.readInt()];
		buf.readBytes(hash);
		var columns = new BlockColumn[256];
		for (int i = 0; i < 256; i++) {
			columns[i] = BlockColumn.fromBuf(buf);
		}
		return new ChunkTile(dimension, x, z, timestamp, dataVersion, hash, columns);
	}
}
