package gjum.minecraft.mapsync.common.mixins.voxelmap;

import com.mamiyaotaru.voxelmap.persistent.CompressibleMapData;
import java.util.concurrent.locks.ReentrantLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@link com.mamiyaotaru.voxelmap.persistent.CachedRegion} */
@Mixin(targets = "com.mamiyaotaru.voxelmap.persistent.CachedRegion", remap = false)
public interface CachedRegionAccessor {

    @Accessor(value = "data", remap = false)
    CompressibleMapData getData();

    @Accessor(value = "liveChunksUpdated", remap = false)
    boolean getLiveChunksUpdated();
    @Accessor(value = "liveChunksUpdated", remap = false)
    void setLiveChunksUpdated(boolean liveChunksUpdated);

    @Accessor(value = "dataUpdated", remap = false)
    boolean isDataUpdated();
    @Accessor(value = "dataUpdated", remap = false)
    void setDataUpdated(boolean dataUpdated);

    @Accessor(value = "loaded", remap = false)
    boolean isLoaded();

    @Accessor(value = "threadLock", remap = false)
    ReentrantLock getThreadLock();

    @Invoker(value = "load", remap = false)
    void load();

}
