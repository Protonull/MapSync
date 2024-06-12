package gjum.minecraft.mapsync.mod.mixins.voxelmap;

import com.mamiyaotaru.voxelmap.VoxelMap;
import gjum.minecraft.mapsync.mod.hooks.voxelmap.VoxelMapHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VoxelMap.class, remap = false)
public abstract class VoxelMapMixin {
    @Inject(
        method = "lateInit",
        at = @At("TAIL"),
        remap = false
    )
    public void mapsync$lateInit(
        final boolean showUnderMenus,
        final boolean isFair,
        final @NotNull CallbackInfo ci
    ) {
        VoxelMapHelper.isModAvailable = true;
    }
}
