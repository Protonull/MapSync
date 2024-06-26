package gjum.minecraft.mapsync.mod.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jetbrains.annotations.NotNull;

public final class Shortcuts {
    public static @NotNull MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        }
        catch (final NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("This should never happen!", impossible);
        }
    }
}
