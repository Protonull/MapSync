package gjum.minecraft.mapsync.mod.mixins.voxelmap;

import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.CompressibleMapData;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CachedRegion.class, remap = false)
public interface CachedRegionAccessor {
    @Accessor(value = "data", remap = false)
    CompressibleMapData mapsync$getData();

    @Accessor(value = "loaded", remap = false)
    boolean mapsync$isLoaded();

    @Invoker(value = "load", remap = false)
    void mapsync$load();

    @Accessor(value = "threadLock", remap = false)
    @NotNull ReentrantLock mapsync$getThreadLock();

    @Accessor(value = "liveChunksUpdated", remap = false)
    void mapsync$setLiveChunksUpdated(
        boolean liveChunksUpdated
    );

    @Accessor(value = "dataUpdated", remap = false)
    void mapsync$setDataUpdated(
        boolean dataUpdated
    );
}
