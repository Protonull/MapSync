package gjum.minecraft.mapsync.mod;

import gjum.minecraft.mapsync.mod.data.ChunkTile;
import gjum.minecraft.mapsync.mod.hooks.journeymap.JourneyMapHelper;
import gjum.minecraft.mapsync.mod.hooks.voxelmap.VoxelMapHelper;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public class RenderQueue {
	private final DimensionState dimensionState;

	private Thread thread;

	private final PriorityBlockingQueue<ChunkTile> queue = new PriorityBlockingQueue<>(18,
			// newest chunks first
			Comparator.comparingLong(ChunkTile::timestamp).reversed());

	public RenderQueue(DimensionState dimensionState) {
		this.dimensionState = dimensionState;
	}

	public int getQueueSize() {
		return queue.size();
	}

	/**
	 * don't push chunks from mc - they're rendered by the installed map mod
	 */
	public synchronized void renderLater(@NotNull ChunkTile chunkTile) {
		queue.add(chunkTile);
		if (thread == null) {
			thread = new Thread(this::renderLoop);
			thread.start();
		}
	}

	public synchronized void shutDown() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	private void renderLoop() {
		try {
			while (true) {
				Thread.sleep(0); // allow stopping via thread.interrupt()

				if (Minecraft.getInstance().level == null) {
					return; // world closed; all queued chunks can't be rendered
				}

				if (!JourneyMapHelper.isJourneyMapNotAvailable && !JourneyMapHelper.isMapping()
						|| !VoxelMapHelper.isVoxelMapNotAvailable && !VoxelMapHelper.isMapping()
				) {
					MapSyncMod.debugLog("render is waiting til map mod is ready");
					Thread.sleep(1000);
					continue;
				}

				var chunkTile = queue.poll();
				if (chunkTile == null) return;

				if (chunkTile.dimension() != Minecraft.getInstance().level.dimension()) {
					MapSyncMod.debugLog("skipping render wrong dim " + chunkTile.chunkPos());
					continue; // mod renderers would render this to the wrong dimension
				}

				// chunks from sync server (live, region) will always be older than mc, so mc will take priority
				if (chunkTile.timestamp() < dimensionState.getChunkTimestamp(chunkTile.chunkPos())) {
					// don't overwrite newer data with older data
					MapSyncMod.debugLog("skipping render outdated " + chunkTile.chunkPos());
				} else {
					boolean voxelRendered = VoxelMapHelper.updateWithChunkTile(chunkTile);
					boolean renderedJM = JourneyMapHelper.updateWithChunkTile(chunkTile);

					MapSyncMod.debugLog("rendered? " + (voxelRendered||renderedJM) + " " + chunkTile.chunkPos() + " queue=" + queue.size());

					if (renderedJM || voxelRendered) {
						dimensionState.setChunkTimestamp(chunkTile.chunkPos(), chunkTile.timestamp());
					} // otherwise, update this chunk again when server sends it again
				}

				// count skipped(outdated) chunks too so DimensionState's "received" vs "rendered" count matches up
				dimensionState.onChunkRenderDone(chunkTile);
			}
		} catch (InterruptedException ignored) {
			// exit silently
		} catch (Throwable err) {
			err.printStackTrace();
		} finally {
			synchronized (this) {
				thread = null;
			}
		}
	}

	public static boolean areAllMapModsMapping() {
		return JourneyMapHelper.isMapping();
	}
}
