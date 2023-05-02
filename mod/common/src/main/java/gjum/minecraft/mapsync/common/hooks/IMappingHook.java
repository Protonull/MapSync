package gjum.minecraft.mapsync.common.hooks;

import gjum.minecraft.mapsync.common.data.ChunkTile;
import org.jetbrains.annotations.NotNull;

public interface IMappingHook {

    /**
     * @return Returns whether this hook is capable of mapping. Consider this
     *         akin to an .isEnabled() combined with an .isWorking().
     */
    boolean isMapping();

    /**
     * Artificially give chunk data to the mapping mod.
     *
     * @return Returns whether the mapping mod was successfully updated.
     */
    boolean updateWithChunkTile(
            final @NotNull ChunkTile chunkTile
    );

}
