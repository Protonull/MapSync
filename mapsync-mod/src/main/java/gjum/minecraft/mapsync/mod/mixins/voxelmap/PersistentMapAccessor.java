package gjum.minecraft.mapsync.mod.mixins.voxelmap;

import com.mamiyaotaru.voxelmap.persistent.CachedRegion;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PersistentMap.class)
public interface PersistentMapAccessor {
    @Accessor(value = "world", remap = false)
    @Nullable ClientLevel mapsync$getWorld();

    @Accessor(value = "cachedRegions", remap = false)
    @NotNull ConcurrentHashMap<String, CachedRegion> mapsync$getCachedRegions();

    @Accessor(value = "cachedRegionsPool", remap = false)
    @NotNull List<CachedRegion> mapsync$getCachedRegionsPool();
}
