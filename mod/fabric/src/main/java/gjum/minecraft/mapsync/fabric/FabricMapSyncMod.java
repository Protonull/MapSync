package gjum.minecraft.mapsync.fabric;

import gjum.minecraft.mapsync.common.MapSyncMod;
import gjum.minecraft.mapsync.common.gui.KeyBinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;

public class FabricMapSyncMod extends MapSyncMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		init();
		KeyBinds.MAPPINGS.forEach(KeyBindingHelper::registerKeyBinding);
		ClientTickEvents.START_CLIENT_TICK.register((final Minecraft client) -> {
			try {
				handleTick();
			}
			catch (final Throwable thrown) {
				logger.warn(thrown);
			}
		});
	}
}
