package gjum.minecraft.mapsync.mod.mixins;

import gjum.minecraft.mapsync.mod.SyncState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SyncStateHolderMixin implements SyncState.Holder {
    @Unique
    private final SyncState mapsync$syncState = new SyncState((ClientPacketListener) (Object) this);

    @Unique
    @Override
    public @NotNull SyncState getMapSyncState() {
        return this.mapsync$syncState;
    }

    @Inject(
        method = "handleLogin",
        at = @At("TAIL")
    )
    protected void mapsync$onLogin(
        final @NotNull CallbackInfo ci
    ) {
        if (Minecraft.getInstance().isSameThread()) {
            getMapSyncState().init();
        }
    }

    @Inject(
        method = "close",
        at = @At("TAIL"),
        remap = false
    )
    protected void mapsync$onClose(
        final @NotNull CallbackInfo ci
    ) {
        getMapSyncState().close();
    }
}
