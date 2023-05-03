package gjum.minecraft.mapsync.common.utilities;

import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public final class JavaHelper {

    /**
     * Convenience function since piping is not a thing in Java (yet)
     */
    public static <T, R> R pipe(
            final T value,
            final @NotNull Function<T, R> computer
    ) {
        return computer.apply(value);
    }

    /**
     * Convenience function to yield a value from multiple lines, inline.
     * Keep an eye on: https://openjdk.org/jeps/447
     */
    public static <T> T yield(
            final @NotNull Supplier<T> supplier
    ) {
        return supplier.get();
    }

}
