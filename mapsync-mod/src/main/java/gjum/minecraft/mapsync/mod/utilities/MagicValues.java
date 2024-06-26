package gjum.minecraft.mapsync.mod.utilities;

import gjum.minecraft.mapsync.mod.MapSyncMod;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

public final class MagicValues {
    public static final String MOD_VERSION = new String(
        readConstant("MOD_VERSION"),
        StandardCharsets.UTF_8
    );

    // SHA1 produces 160-bit (20-byte) hashes
    // https://en.wikipedia.org/wiki/SHA-1
    public static final int SHA1_HASH_LENGTH = 20;

    // ============================================================
    // Helpers
    // ============================================================

    public static void forceClassLoad() {
        // This only exists to force the class to load at a specific time.
    }

    private static byte @NotNull [] readConstant(
        final @NotNull String name
    ) {
        try (final InputStream in = MapSyncMod.class.getResourceAsStream("/constants/mapsync/" + name)) {
            return in.readAllBytes(); // Ignore highlighter
        }
        catch (final IOException | NullPointerException thrown) {
            throw new IllegalStateException("Could not load constant resource [" + name + "]!", thrown);
        }
    }
}
