package gjum.minecraft.mapsync.common.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeyBinds {

    private static final KeyMapping OPEN_GUI_KEY;

    public static final List<KeyMapping> MAPPINGS = List.of(
            OPEN_GUI_KEY = new KeyMapping(
                    "key.map-sync.openGui",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_COMMA,
                    "category.map-sync"
            )
    );

    public static void handleTick() {
        while (OPEN_GUI_KEY.consumeClick()) {
            final var mc = Minecraft.getInstance();
            mc.setScreen(new ModGui(mc.screen));
        }
    }

}
