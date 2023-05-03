package gjum.minecraft.mapsync.forge;

import gjum.minecraft.mapsync.common.MapSyncMod;
import gjum.minecraft.mapsync.common.gui.KeyBinds;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("mapsync")
public class ForgeMapSyncMod extends MapSyncMod {
	public ForgeMapSyncMod() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener((final FMLClientSetupEvent event) -> {
			init();
			KeyBinds.MAPPINGS.forEach(ClientRegistry::registerKeyBinding);
		});
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onClientTick(
			final TickEvent.ClientTickEvent event
	) {
		try {
			if (event.phase == TickEvent.Phase.START) {
				handleTick();
			}
		}
		catch (final Throwable thrown) {
			logger.warn(thrown);
		}
	}
}
