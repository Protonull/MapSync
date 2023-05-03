package gjum.minecraft.mapsync.common.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public final class SHA1 {
    private static final Object $LOCK = new Object[0];
    private static final MessageDigest DIGEST;
    static {
        try {
            // SHA-1 is faster than SHA-256, and other algorithms are not required to be implemented in every JVM
            DIGEST = MessageDigest.getInstance("SHA-1");
        }
        catch (final NoSuchAlgorithmException thrown) {
            throw new IllegalStateException("SHA1 hashing is required!");
        }
    }

    public static byte @NotNull [] hash(final @NotNull Consumer<@NotNull MessageDigest> hasher) {
        synchronized ($LOCK) {
            DIGEST.reset();
            hasher.accept(DIGEST);
            return DIGEST.digest();
        }
    }
}
