package gjum.minecraft.mapsync.mod.hooks.voxelmap;

import gjum.minecraft.mapsync.mod.data.ChunkTile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class VoxelMapHelper {
	@ApiStatus.Internal
	public static boolean isModAvailable = false;

	public static boolean isMapping() {
		return isModAvailable && VoxelMapHelperReal.isMapping();
	}

	public static boolean updateWithChunkTile(
		final @NotNull ChunkTile chunkTile
	) {
		return isModAvailable && VoxelMapHelperReal.updateWithChunkTile(chunkTile);
	}
}
