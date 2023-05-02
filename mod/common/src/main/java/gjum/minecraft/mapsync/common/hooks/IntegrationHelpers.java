package gjum.minecraft.mapsync.common.hooks;

import gjum.minecraft.mapsync.common.data.ChunkTile;
import gjum.minecraft.mapsync.common.hooks.journeymap.JourneyMapHook;
import gjum.minecraft.mapsync.common.hooks.voxelmap.VoxelMapHook;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public final class IntegrationHelpers {

    public static final IMappingHook JOURNEYMAP_HOOK;
    public static final IMappingHook VOXELMAP_HOOK;

    public static final List<IMappingHook> HOOKS = List.of(
            JOURNEYMAP_HOOK = new JourneyMapHook(),
            VOXELMAP_HOOK = new VoxelMapHook()
    );

    /**
     * Returns true if any mapping hook can map.
     */
    public static boolean areAnyMappingHooksReady() {
        for (final IMappingHook hook : HOOKS) {
            if (hook.isMapping()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a chunk tile to all mapping hooks. If any hook returns true then
     * this also returns true.
     */
    public static boolean updateHooksWithChunkTile(
            final @NotNull ChunkTile chunkTile
    ) {
        boolean didAtLeastOneHookUpdateSuccessfully = false;
        for (final IMappingHook hook : HOOKS) {
            if (hook.updateWithChunkTile(chunkTile)) {
                didAtLeastOneHookUpdateSuccessfully = true;
            }
        }
        return didAtLeastOneHookUpdateSuccessfully;
    }

    /**
     * Pass in a series of class paths. It'll return an unmodifiable list of
     * class-doesn't-exist related errors. An empty list means that none of
     * those errors were thrown and thus can infer that the classes were found.
     */
    public static @NotNull List<@NotNull Throwable> testClassesExist(
            final @NotNull String... classes
    ) {
        return Stream.of(classes)
                .map((final String clazz) -> {
                    try {
                        Class.forName(clazz);
                        return null;
                    }
                    catch (final NoClassDefFoundError | ClassNotFoundException thrown) {
                        return thrown;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

}
