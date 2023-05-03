package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.utilities.JavaHelper;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record S2C_RegionTimestampsPacket(
        @NotNull String dimension,
        @NotNull List<@NotNull RegionTimestamp> timestamps
) implements IPacket {
    public static final int PACKET_ID = 7;

    public S2C_RegionTimestampsPacket {
        timestamps = List.copyOf(timestamps);
    }

    public S2C_RegionTimestampsPacket(final @NotNull ByteBuf buffer) {
        this(
                IPacket.readString(buffer),
                JavaHelper.pipe(buffer.readShort(), (length) -> {
                    final var timestamps = new RegionTimestamp[length];
                    for (short i = 0; i < length; i++) {
                        timestamps[i] = new RegionTimestamp(
                                buffer.readShort(),
                                buffer.readShort(),
                                buffer.readLong()
                        );
                    }
                    return List.of(timestamps);
                })
        );
    }

    public record RegionTimestamp(
            short x,
            short z,
            long timestamp
    ) { }
}
