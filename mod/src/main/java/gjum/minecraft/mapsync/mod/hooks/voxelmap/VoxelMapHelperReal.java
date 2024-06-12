package gjum.minecraft.mapsync.mod.hooks.voxelmap;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.CompressibleMapData;
import com.mamiyaotaru.voxelmap.persistent.EmptyCachedRegion;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import gjum.minecraft.mapsync.mod.data.BlockColumn;
import gjum.minecraft.mapsync.mod.data.BlockInfo;
import gjum.minecraft.mapsync.mod.data.ChunkTile;
import gjum.minecraft.mapsync.mod.mixins.voxelmap.CachedRegionAccessor;
import gjum.minecraft.mapsync.mod.mixins.voxelmap.PersistentMapAccessor;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoxelMapHelperReal {
	static boolean isMapping() {
		return ((PersistentMapAccessor) VoxelConstants.getVoxelMapInstance().getPersistentMap()).mapsync$getWorld() != null;
	}

	// TODO update multiple chunks in one region at once
	// TODO which thread should this run on?
	static boolean updateWithChunkTile(
		final @NotNull ChunkTile chunkTile
	) {
		final PersistentMap map = VoxelConstants.getVoxelMapInstance().getPersistentMap();
		final var mapAccessor = (PersistentMapAccessor) map;
		final ClientLevel currentLevel = mapAccessor.mapsync$getWorld();
		if (currentLevel == null) {
			return false;
		}

		final int regionX = chunkTile.x() >> 4;
		final int regionZ = chunkTile.z() >> 4;

		final WaypointManager waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
		final String worldName = waypointManager.getCurrentWorldName();
		final String subWorldName = waypointManager.getCurrentSubworldDescriptor(false);

		final String regionKey = regionX + "," + regionZ;

		@Nullable CachedRegion region = null;

		/**
		 * This is sketch, but it's what VoxelMap itself does here:
		 * {@link com.mamiyaotaru.voxelmap.persistent.PersistentMap#getRegions(int, int, int, int)}
		 */
		final ConcurrentHashMap<String, CachedRegion> cachedRegions = mapAccessor.mapsync$getCachedRegions();
		synchronized (cachedRegions) {
			region = cachedRegions.get(regionKey);
			// could be race condition if the region is not fully loaded at this point
			if (region == null || region instanceof EmptyCachedRegion) {
				cachedRegions.put(regionKey, region = new CachedRegion(
					map,
					regionKey,
					currentLevel,
					worldName,
					subWorldName,
					regionX,
					regionZ
				));

				final List<CachedRegion> cachedRegionsPool = mapAccessor.mapsync$getCachedRegionsPool();
				synchronized (cachedRegionsPool) {
					cachedRegionsPool.add(region);
				}
			}
		}

		final var regionAccessor = (CachedRegionAccessor) region;
		if (!regionAccessor.mapsync$isLoaded()) {
			regionAccessor.mapsync$load();
		}

		final var biomeRegistry = currentLevel.registryAccess().registry(Registries.BIOME).orElseThrow();

		final ReentrantLock lock = regionAccessor.mapsync$getThreadLock(); lock.lock(); try {
			final CompressibleMapData data = regionAccessor.mapsync$getData();

			final int x0 = (chunkTile.x() << 16) & 0xFF;
			final int z0 = (chunkTile.z() << 16) & 0xFF;

			int i = 0;
			for (int z = z0; z < z0 + 16; ++z) for (int x = x0; x < x0 + 16; ++x) {
				final BlockColumn blockColumn = chunkTile.columns()[i++];

				data.setBiomeID(x, z, biomeRegistry.getId(blockColumn.biome()));

				final int light = 0xF0 | blockColumn.light();
				data.setLight(x, z, light);
				data.setTransparentLight(x, z, light);
				data.setFoliageLight(x, z, light);
				data.setOceanFloorLight(x, z, light);

				BlockInfo transparent = newAirBlock();
				BlockInfo foliage = newAirBlock();
				BlockInfo surface = newAirBlock();
				BlockInfo seafloor = newAirBlock();

				final List<BlockInfo> blockColumnLayers = blockColumn.layers();

				// XXX
				final BlockInfo zerothBlock = blockColumnLayers.get(0);
				if (blockColumnLayers.size() > 1) {
					transparent = zerothBlock;
				}
				surface = blockColumnLayers.get(blockColumnLayers.size() - 1);
				// trees hack
				if (zerothBlock.state().is(BlockTags.LEAVES)) {
					surface = zerothBlock;
				}

				data.setTransparentHeight(x, z, transparent.y());
				data.setTransparentBlockstate(x, z, transparent.state());
				data.setFoliageHeight(x, z, foliage.y());
				data.setFoliageBlockstate(x, z, foliage.state());
				data.setHeight(x, z, surface.y());
				data.setBlockstate(x, z, surface.state());
				data.setOceanFloorHeight(x, z, seafloor.y());
				data.setOceanFloorBlockstate(x, z, seafloor.state());
			}

			regionAccessor.mapsync$setLiveChunksUpdated(true);
			regionAccessor.mapsync$setDataUpdated(true);

			// render imagery
			region.refresh(false);
		}
		finally {
			lock.unlock();
		}
		return true;
	}

	private static @NotNull BlockInfo newAirBlock() {
		return new BlockInfo(0, Blocks.AIR.defaultBlockState());
	}
}
