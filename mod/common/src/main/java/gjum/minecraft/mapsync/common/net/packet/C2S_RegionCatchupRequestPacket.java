package gjum.minecraft.mapsync.common.net.packet;

import gjum.minecraft.mapsync.common.data.RegionPos;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * This is a response to the RegionTimestampsPacket. This packet is a request
 * to the server to be updated on regions that the server has newer data for.
 *
 * @param dimension The dimension of the regions.
 * @param regions The regions where the server has newer data.
 */
public record C2S_RegionCatchupRequestPacket(
        @NotNull String dimension,
        @NotNull List<@NotNull RegionPos> regions
) implements IPacket {
    public static final int PACKET_ID = 8;

    public C2S_RegionCatchupRequestPacket {
        regions = List.copyOf(regions);
    }

    @Override
    public void write(final @NotNull ByteBuf buffer) {
        IPacket.writeString(buffer, this.dimension);
        buffer.writeShort(this.regions.size());
        for (final RegionPos region : this.regions) {
            buffer.writeShort(region.x());
            buffer.writeShort(region.z());
        }
    }
}
