package gjum.minecraft.mapsync.mod.hooks.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import gjum.minecraft.mapsync.mod.ModGui;
import org.jetbrains.annotations.NotNull;

/**
 * Adds support for https://github.com/TerraformersMC/ModMenu (Fabric only)
 */
public class ModMenuHook implements ModMenuApi {
	public @NotNull ConfigScreenFactory<ModGui> getModConfigScreenFactory() {
		return ModGui::new;
	}
}
