package gjum.minecraft.mapsync.common.hooks.journeymap;

import gjum.minecraft.mapsync.common.data.BlockColumn;
import gjum.minecraft.mapsync.common.data.BlockInfo;
import gjum.minecraft.mapsync.common.data.ChunkTile;
import java.util.Objects;
import journeymap.client.model.MapType;
import journeymap.client.model.NBTChunkMD;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;

final class TileChunkMD extends NBTChunkMD {
    private final ChunkTile chunkTile;

    public TileChunkMD(
            final @NotNull ChunkTile chunkTile
    ) {
        super(
                new LevelChunk(
                        Objects.requireNonNull(Minecraft.getInstance().level),
                        chunkTile.chunkPos()
                ),
                chunkTile.chunkPos(),
                null, // all accessing methods are overridden
                MapType.day(chunkTile.dimension()) // just has to not be `underground`
        );
        this.chunkTile = chunkTile;
    }

    @Override
    public boolean hasChunk() {
        return true;
    }

    private BlockColumn getCol(
            final int x,
            final int z
    ) {
        final int xic = x & 0xf;
        final int zic = z & 0xf;
        return this.chunkTile.columns()[xic + zic * 16];
    }

    private BlockColumn getCol(
            final @NotNull BlockPos pos
    ) {
        return getCol(pos.getX(), pos.getZ());
    }

    @Override
    public BlockState getBlockState(
            final @NotNull BlockPos pos
    ) {
        final var layers = getCol(pos.getX(), pos.getZ()).layers();
        BlockInfo prevLayer = null;
        // note that layers are ordered top-down
        for (final BlockInfo layer : layers) {
            if (layer.y() == pos.getY()) {
                return layer.state();
            }
            if (layer.y() < pos.getY()) {
                // top of layer is below pos, so pos is inside prevLayer
                if (prevLayer == null) {
                    return Blocks.AIR.defaultBlockState(); // first layer is already below pos
                }
                return prevLayer.state();
            }
            prevLayer = layer;
        }
        if (layers.isEmpty()) {
            return Blocks.AIR.defaultBlockState();
        }
        return layers.get(layers.size() - 1).state();
    }

    @Override
    public Integer getGetLightValue(
            final @NotNull BlockPos pos
    ) {
        return getCol(pos.getX(), pos.getZ()).light();
    }

    @Override
    public Integer getTopY(
            final @NotNull BlockPos pos
    ) {
        return getCol(pos.getX(), pos.getZ()).layers().get(0).y();
    }

    @Override
    public int getHeight(
            final @NotNull BlockPos pos
    ) {
        return this.getTopY(pos);
    }

    @Override
    public Biome getBiome(
            final @NotNull BlockPos pos
    ) {
        return getCol(pos).biome();
    }
}
