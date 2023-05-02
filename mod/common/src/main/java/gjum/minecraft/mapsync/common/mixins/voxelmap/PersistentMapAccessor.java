package gjum.minecraft.mapsync.common.mixins.voxelmap;

import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@link com.mamiyaotaru.voxelmap.persistent.PersistentMap} */
@Mixin(targets = "com.mamiyaotaru.voxelmap.persistent.PersistentMap")
public interface PersistentMapAccessor {

    @Accessor(value = "world", remap = false)
    ClientLevel getWorld();

    @Accessor(value = "cachedRegionsPool", remap = false)
    List<CachedRegion> getCachedRegionsPool();

    @Accessor(value = "cachedRegions", remap = false)
    ConcurrentHashMap getCachedRegions();

}
