package gjum.minecraft.mapsync.common.hooks.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.CompressibleMapData;
import com.mamiyaotaru.voxelmap.persistent.EmptyCachedRegion;
import gjum.minecraft.mapsync.common.data.BlockInfo;
import gjum.minecraft.mapsync.common.data.ChunkTile;
import gjum.minecraft.mapsync.common.hooks.IMappingHook;
import gjum.minecraft.mapsync.common.hooks.IntegrationHelpers;
import gjum.minecraft.mapsync.common.mixins.voxelmap.CachedRegionAccessor;
import gjum.minecraft.mapsync.common.mixins.voxelmap.PersistentMapAccessor;
import gjum.minecraft.mapsync.common.utilities.Shortcuts;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoxelMapHook implements IMappingHook {
	private static final boolean isVoxelMapAvailable = IntegrationHelpers.testClassesExist(
			"com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap"
	).isEmpty();

	@Override
	public boolean isMapping() {
		if (!isVoxelMapAvailable) {
			return false;
		}
		final PersistentMapAccessor mapAccessor = Objects.requireNonNull(
				(PersistentMapAccessor) AbstractVoxelMap.getInstance().getPersistentMap(),
				"PersistentMapAccessor didn't access!"
		);
		return mapAccessor.getWorld() != null;
	}

	// TODO update multiple chunks in one region at once
	// TODO which thread should this run on?
	@Override
	public boolean updateWithChunkTile(
			final @NotNull ChunkTile chunkTile
	) {
		try {
			if (!isMapping()) {
				return false;
			}

			var region = getRegion(
					chunkTile.x() >> 4,
					chunkTile.z() >> 4
			);

			final var regionAccessor = Objects.requireNonNull(
					(CachedRegionAccessor) region,
					"CachedRegionAccessor didn't access!"
			);

			final var lock = regionAccessor.getThreadLock();
			lock.lock();
			try {
				final var mapData = regionAccessor.getData();

				final int x0 = (chunkTile.x() * 16) & 0xff;
				final int z0 = (chunkTile.z() * 16) & 0xff;

				final var biomeRegistry = Shortcuts.getBiomeRegistry();

				int i = 0;
				for (int z = z0; z < z0 + 16; ++z) {
					for (int x = x0; x < x0 + 16; ++x) {
						final var col = chunkTile.columns()[i++];

						mapData.setBiomeID(x, z, biomeRegistry.getId(col.biome()));

						final int light = 0xf0 | col.light();
						mapData.setTransparentLight(x, z, light);
						mapData.setFoliageLight(x, z, light);
						mapData.setLight(x, z, light);
						mapData.setOceanFloorLight(x, z, light);

						setLayerStates(mapData, x, z, col.layers());
					}
				}

				regionAccessor.setLiveChunksUpdated(true);
				regionAccessor.setDataUpdated(true);

				// render imagery
				region.refresh(false);
			}
			finally {
				lock.unlock();
			}

			return true;
		}
		catch (final Throwable thrown) {
			thrown.printStackTrace();
			return false;
		}
	}

	private static final BlockInfo EMPTY = new BlockInfo(0, Blocks.AIR.defaultBlockState());

	private static void setLayerStates(
			final @NotNull CompressibleMapData mapData,
			final int x,
			final int z,
			final @NotNull List<@NotNull BlockInfo> layers
	) {
		BlockInfo transparent = EMPTY;
		BlockInfo foliage = EMPTY;
		BlockInfo surface = EMPTY;
		BlockInfo seafloor = EMPTY;

		// XXX
		if (layers.size() > 1) transparent = layers.get(0);
		surface = layers.get(layers.size() - 1);
		// trees hack
		if (layers.get(0).state().getMaterial() == Material.LEAVES) {
			surface = layers.get(0);
		}

		mapData.setTransparentHeight(x, z, transparent.y());
		mapData.setTransparentBlockstate(x, z, transparent.state());
		mapData.setFoliageHeight(x, z, foliage.y());
		mapData.setFoliageBlockstate(x, z, foliage.state());
		mapData.setHeight(x, z, surface.y());
		mapData.setBlockstate(x, z, surface.state());
		mapData.setOceanFloorHeight(x, z, seafloor.y());
		mapData.setOceanFloorBlockstate(x, z, seafloor.state());
	}

	private static @NotNull CachedRegion getRegion(
			final int regionX,
			final int regionZ
	) {
		var vm = AbstractVoxelMap.getInstance();

		final var map = vm.getPersistentMap();
		final PersistentMapAccessor mapAccessor = Objects.requireNonNull(
				(PersistentMapAccessor) map,
				"PersistentMapAccessor didn't access!"
		);

		final var cachedRegions = mapAccessor.getCachedRegions();
		final var cachedRegionsPool = mapAccessor.getCachedRegionsPool();
		final var dimension = mapAccessor.getWorld();

		final String dimensionName = vm.getWaypointManager().getCurrentWorldName();
		final String subDimensionName = vm.getWaypointManager().getCurrentSubworldDescriptor(false);
		final String key = regionX + "," + regionZ;

		@Nullable CachedRegion region;

		/**
		 * This synchronized-block matches voxelmap's own internal logic, see:
		 * {@link com.mamiyaotaru.voxelmap.persistent.PersistentMap#getRegions(int, int, int, int)}
		 */
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (cachedRegions) {
			region = (CachedRegion) cachedRegions.get(key);
			// could be race condition if the region is not fully loaded at this point
			if (region == null || region instanceof EmptyCachedRegion) {
				region = new CachedRegion(map, key, dimension, dimensionName, subDimensionName, regionX, regionZ);

				cachedRegions.put(key, region);

				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (cachedRegionsPool) {
					cachedRegionsPool.add(region);
				}
			}
		}

		final var regionAccessor = Objects.requireNonNull(
				(CachedRegionAccessor) region,
				"CachedRegionAccessor didn't access!"
		);
		// TODO which thread should this run on?
		//      (Answer?) Looking through voxelmap code, calls to "load" are
		//                always within a ThreadManager.executorService.execute()
		//                or similar. Perhaps that's the answer?
		if (!regionAccessor.isLoaded()) {
			// TODO This will generate a massive amount of errors (that cannot
			//      be try-catched) then stop presumably because the world has
			//      finished loading. Perhaps there should be another condition
			//      waiting for all loading to be finished.
			regionAccessor.load();
		}

		return region;
	}
}
