package gjum.minecraft.mapsync.common.utilities;

import it.unimi.dsi.fastutil.objects.Object2LongAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class ErrorRateLimiting {
    public static final long DELAY = 10_000L;

    public interface ErrorPrinter {
        void print(@NotNull Throwable thrown);
    }
    public static final ErrorPrinter DEFAULT_PRINTER = Throwable::printStackTrace;
    public static ErrorPrinter PRINTER = DEFAULT_PRINTER;

    private static final Object2LongMap<String> LAST_TIME_SEEN_ERROR;
    static {
        LAST_TIME_SEEN_ERROR = new Object2LongAVLTreeMap<>();
        LAST_TIME_SEEN_ERROR.defaultReturnValue(0L);
    }

    public static void printRateLimitedError(final @NotNull Throwable thrown) {
        final long now = System.currentTimeMillis();
        final String message = Objects.requireNonNullElse(thrown.getMessage(), "");
        if ((now - DELAY) > LAST_TIME_SEEN_ERROR.getLong(message)) {
            // Do not print the error. It's being rate limited.
            return;
        }
        LAST_TIME_SEEN_ERROR.put(message, now);
        Objects.requireNonNullElse(PRINTER, DEFAULT_PRINTER).print(thrown);
    }
}
