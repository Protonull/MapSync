package gjum.minecraft.mapsync.common.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.NotNull;

public final class Shortcuts {

    public static @NotNull Registry<Biome> getBiomeRegistry() {
        final ClientLevel dimension = Minecraft.getInstance().level;
        if (dimension == null) {
            throw new IllegalStateException("Cannot retrieve biome registry: client is not in a dimension!");
        }
        return dimension.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
    }

}
