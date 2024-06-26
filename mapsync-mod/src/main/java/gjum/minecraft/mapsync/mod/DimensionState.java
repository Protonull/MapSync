package gjum.minecraft.mapsync.mod;

import gjum.minecraft.mapsync.mod.data.CatchupChunk;
import gjum.minecraft.mapsync.mod.data.ChunkTile;
import gjum.minecraft.mapsync.mod.data.RegionPos;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * contains any background processes and data structures, to be able to easily tear down when leaving the dimension
 */
public class DimensionState {
	private static final Minecraft mc = Minecraft.getInstance();

	public final ResourceKey<Level> dimension;
	boolean hasShutDown = false;

	private final DimensionChunkMeta chunkMeta;
	private final RenderQueue renderQueue;
	private final CatchupLogic catchup;
	private int numChunksReceived = 0;
	private int numChunksRendered = 0;

	DimensionState(String mcServerName, ResourceKey<Level> dimension) {
		this.dimension = dimension;
		String dimensionName = dimension.location().toString();
		chunkMeta = new DimensionChunkMeta(mcServerName, dimensionName);
		renderQueue = new RenderQueue(this);
		catchup = new CatchupLogic(this);
	}

	public synchronized void shutDown() {
		if (hasShutDown) return;
		hasShutDown = true;
		renderQueue.shutDown();
	}

	public long getOldestChunkTsInRegion(RegionPos regionPos) {
		return chunkMeta.getOldestChunkTsInRegion(regionPos);
	}

	public long getChunkTimestamp(ChunkPos chunkPos) {
		return chunkMeta.getTimestamp(chunkPos);
	}

	public void setChunkTimestamp(ChunkPos chunkPos, long timestamp) {
		chunkMeta.setTimestamp(chunkPos, timestamp);
	}

	public int getNumChunksReceived() {
		return numChunksReceived;
	}

	public int getNumChunksRendered() {
		return numChunksRendered;
	}

	public int getRenderQueueSize() {
		return renderQueue.getQueueSize();
	}

	public void addCatchupChunks(List<CatchupChunk> catchupChunks) {
		catchup.addCatchupChunks(catchupChunks);
	}

	public void processSharedChunk(ChunkTile chunkTile) {
		if (hasShutDown) return;
		if (mc.level == null) return;
		if (dimension != mc.level.dimension()) {
			MapSyncMod.debugLog("Dropping chunk tile: mc changed dimension");
			shutDown(); // player entered different dimension
			return;
		}

		if (chunkTile.dimension() != dimension) {
			MapSyncMod.debugLog("Dropping chunk tile: wrong dimension "
					+ chunkTile.dimension() + " wanted " + dimension);
			return; // don't render tile to the wrong dimension
		}

		++numChunksReceived;

		catchup.handleSharedChunkReceived(chunkTile);

		if (mc.level.getChunkSource().hasChunk(chunkTile.x(), chunkTile.z())) {
			// don't update loaded chunks
			MapSyncMod.debugLog("Dropping chunk tile: loaded in world");
			++numChunksRendered; // count skipped(loaded) chunks too so the "received" vs "rendered" count matches up
			return;
		}

		renderQueue.renderLater(chunkTile);
	}

	public void onChunkRenderDone(ChunkTile chunkTile) {
		catchup.maybeRequestMoreCatchup();
		++numChunksRendered;
	}

	public void onTick() {
		catchup.maybeRequestMoreCatchup();
	}
}
