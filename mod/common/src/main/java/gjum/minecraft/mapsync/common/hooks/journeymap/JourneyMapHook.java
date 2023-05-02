package gjum.minecraft.mapsync.common.hooks.journeymap;

import gjum.minecraft.mapsync.common.data.ChunkTile;
import gjum.minecraft.mapsync.common.hooks.IMappingHook;
import gjum.minecraft.mapsync.common.hooks.IntegrationHelpers;
import java.util.Set;
import journeymap.client.JourneymapClient;
import journeymap.client.io.FileHandler;
import journeymap.client.model.MapType;
import journeymap.client.model.RegionCoord;
import journeymap.common.nbt.RegionData;
import journeymap.common.nbt.RegionDataStorageHandler;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public final class JourneyMapHook implements IMappingHook {
    private static final boolean isJourneyMapAvailable = IntegrationHelpers.testClassesExist(
            "journeymap.client.JourneymapClient",
            "journeymap.client.model.NBTChunkMD"
    ).isEmpty();

    private static final Set<MapType.Name> MAP_TYPES = Set.of(
            MapType.Name.day,
            MapType.Name.biome,
            MapType.Name.topo
    );

    @Override
    public boolean isMapping() {
        return isJourneyMapAvailable && JourneymapClient.getInstance().isMapping();
    }

    @Override
    public boolean updateWithChunkTile(
            final @NotNull ChunkTile chunkTile
    ) {
        if (!isMapping()) {
            return false;
        }

        final var renderController = JourneymapClient.getInstance().getChunkRenderController();
        if (renderController == null) {
            return false;
        }

        final var chunkMd = new TileChunkMD(chunkTile);

        final var regionCoord = RegionCoord.fromChunkPos(
                FileHandler.getJMWorldDir(Minecraft.getInstance()),
                MapType.day(chunkTile.dimension()), // type doesn't matter, only dimension is used
                chunkMd.getCoord().x,
                chunkMd.getCoord().z
        );

        final RegionData regionData = RegionDataStorageHandler.getInstance().getRegionData(
                new RegionDataStorageHandler.Key(
                        regionCoord,
                        MapType.day(chunkTile.dimension())
                )
        );

        boolean didAllSucceed = true;
        for (final MapType.Name type : MAP_TYPES) {
            final boolean succeeded = renderController.renderChunk(
                    regionCoord,
                    MapType.from(type, null, chunkTile.dimension()),
                    chunkMd,
                    regionData
            );
            if (!succeeded) {
                System.out.println("Failed rendering [" + type.name() + "] at " + chunkTile.chunkPos());
                didAllSucceed = false;
            }
        }

        return didAllSucceed;
    }
}
