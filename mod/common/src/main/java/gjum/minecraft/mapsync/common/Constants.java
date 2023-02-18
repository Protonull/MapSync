package gjum.minecraft.mapsync.common;

import java.util.Properties;

public final class Constants {

    public static final String VERSION;

    static {
        final var properties = new Properties();
        try {
            properties.load(MapSyncMod.class.getResourceAsStream("/constants.properties"));
        }
        catch (final Throwable thrown) {
            throw new IllegalStateException(thrown);
        }
        VERSION = properties.getProperty("version");
    }

}
