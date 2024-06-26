package gjum.minecraft.mapsync.mod;

import com.mojang.blaze3d.platform.InputConstants;
import gjum.minecraft.mapsync.mod.config.ModConfig;
import gjum.minecraft.mapsync.mod.config.ServerConfig;
import gjum.minecraft.mapsync.mod.data.CatchupChunk;
import gjum.minecraft.mapsync.mod.data.ChunkTile;
import gjum.minecraft.mapsync.mod.data.RegionPos;
import gjum.minecraft.mapsync.mod.network.SyncClient;
import gjum.minecraft.mapsync.mod.network.packet.ClientboundChunkTimestampsResponsePacket;
import gjum.minecraft.mapsync.mod.network.packet.ClientboundRegionTimestampsPacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundCatchupRequestPacket;
import gjum.minecraft.mapsync.mod.network.packet.ServerboundChunkTimestampsRequestPacket;
import gjum.minecraft.mapsync.mod.utilities.MagicValues;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class MapSyncMod implements ClientModInitializer {
	private static final Minecraft mc = Minecraft.getInstance();
	public static final Logger LOGGER = LogManager.getLogger(MapSyncMod.class);

	public static ModConfig CONFIG;
	private static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
			"key.map-sync.openGui",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_COMMA,
			"category.map-sync"
	);

	/**
	 * Tracks state and render thread for current mc dimension.
	 * Never access this directly; always go through `getDimensionState()`.
	 */
	private @Nullable DimensionState dimensionState;

	@Override
	public void onInitializeClient() {
		MagicValues.forceClassLoad();

		KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEY);

		CONFIG = ModConfig.load();
		CONFIG.saveNow(); // creates the default file if it doesn't exist yet

		ClientTickEvents.START_CLIENT_TICK.register(this::handleTick);
	}

	private void handleTick(
		final @NotNull Minecraft minecraft
	) {
		while (OPEN_GUI_KEY.consumeClick()) {
			minecraft.setScreen(new ModGui(minecraft.screen));
		}

		var dimensionState = getDimensionState();
		if (dimensionState != null) dimensionState.onTick();
	}

	public void handleRespawn(ClientboundRespawnPacket packet) {
		debugLog("handleRespawn");
		// TODO tell sync server to only send chunks for this dimension now
	}

	public Optional<SyncState> getSyncState() {
		if (Minecraft.getInstance().getConnection() instanceof final SyncState.Holder holder) {
			return Optional.of(holder.getMapSyncState());
		}
		return Optional.empty();
	}

	/**
	 * only null when not connected to a server
	 */
	public @Nullable ServerConfig getServerConfig() {
		return getSyncState().map((syncState) -> syncState.serverConfig).orElse(null);
	}

	public @NotNull List<SyncClient> getSyncClients() {
		return getSyncState().map((syncState) -> Collections.unmodifiableList(syncState.syncConnections)).orElseGet(List::of);
	}

	/**
	 * for current dimension
	 */
	public @Nullable DimensionState getDimensionState() {
		if (mc.level == null) return null;
		var serverConfig = getServerConfig();
		if (serverConfig == null) return null;

		if (dimensionState != null && dimensionState.dimension != mc.level.dimension()) {
			shutDownDimensionState();
		}
		if (dimensionState == null || dimensionState.hasShutDown) {
			dimensionState = new DimensionState(serverConfig.gameAddress, mc.level.dimension());
		}
		return dimensionState;
	}

	private void shutDownDimensionState() {
		if (dimensionState != null) {
			dimensionState.shutDown();
			dimensionState = null;
		}
	}

	/**
	 * an entire chunk was received from the mc server;
	 * send it to the map data server right away.
	 */
	public void handleMcFullChunk(int cx, int cz) {
		// TODO batch this up and send multiple chunks at once

		if (mc.level == null) return;
		// TODO disable in nether (no meaningful "surface layer")
		var dimensionState = getDimensionState();
		if (dimensionState == null) return;

		debugLog("received mc chunk: " + cx + "," + cz);

		var chunkTile = Cartography.chunkTileFromLevel(mc.level, cx, cz);

		// TODO handle journeymap skipping chunks due to rate limiting - probably need mixin on render function
		if (RenderQueue.areAllMapModsMapping()) {
			dimensionState.setChunkTimestamp(chunkTile.chunkPos(), chunkTile.timestamp());
		}
		for (SyncClient client : getSyncClients()) {
			client.sendChunkTile(chunkTile);
		}
	}

	/**
	 * part of a chunk changed, and the chunk is likely to change again soon,
	 * so a ChunkTile update is queued, instead of updating instantly.
	 */
	public void handleMcChunkPartialChange(int cx, int cz) {
		// TODO update ChunkTile in a second or so; remember dimension in case it changes til then
	}

	public void handleSyncServerEncryptionSuccess() {
		debugLog("tcp encrypted");
		// TODO tell server our current dimension
	}

	public void handleRegionTimestamps(ClientboundRegionTimestampsPacket packet, SyncClient client) {
		DimensionState dimension = getDimensionState();
		if (dimension == null) return;
		if (!dimension.dimension.location().toString().equals(packet.getDimension())) {
			return;
		}
		var outdatedRegions = new ArrayList<RegionPos>();
		for (var regionTs : packet.getTimestamps()) {
			var regionPos = new RegionPos(regionTs.x(), regionTs.z());
			long oldestChunkTs = dimension.getOldestChunkTsInRegion(regionPos);
			boolean requiresUpdate = regionTs.timestamp() > oldestChunkTs;

			debugLog("region " + regionPos
					+ (requiresUpdate ? " requires update." : " is up to date.")
					+ " oldest client chunk ts: " + oldestChunkTs
					+ ", newest server chunk ts: " + regionTs.timestamp());

			if (requiresUpdate) {
				outdatedRegions.add(regionPos);
			}
		}

		client.send(new ServerboundChunkTimestampsRequestPacket(packet.getDimension(), outdatedRegions));
	}

	public void handleSharedChunk(ChunkTile chunkTile) {
		debugLog("received shared chunk: " + chunkTile.chunkPos());
		for (SyncClient syncClient : getSyncClients()) {
			syncClient.setServerKnownChunkHash(chunkTile.chunkPos(), chunkTile.dataHash());
		}

		var dimensionState = getDimensionState();
		if (dimensionState == null) return;
		dimensionState.processSharedChunk(chunkTile);
	}

	public void handleCatchupData(ClientboundChunkTimestampsResponsePacket packet) {
		var dimensionState = getDimensionState();
		if (dimensionState == null) return;
		debugLog("received catchup: " + packet.chunks.size() + " " + packet.chunks.get(0).syncClient.address);
		dimensionState.addCatchupChunks(packet.chunks);
	}

	public void requestCatchupData(List<CatchupChunk> chunks) {
		if (chunks == null || chunks.isEmpty()) {
			debugLog("not requesting more catchup: null/empty");
			return;
		}

		debugLog("requesting more catchup: " + chunks.size());
		var byServer = new HashMap<String, List<CatchupChunk>>();
		for (CatchupChunk chunk : chunks) {
			var list = byServer.computeIfAbsent(chunk.syncClient.address, (a) -> new ArrayList<>());
			list.add(chunk);
		}
		for (List<CatchupChunk> chunksForServer : byServer.values()) {
			SyncClient client = chunksForServer.get(0).syncClient;
			client.send(new ServerboundCatchupRequestPacket(chunksForServer));
		}
	}

	public static void debugLog(String msg) {
		// we could also make use of slf4j's debug() but I don't know how to reconfigure that at runtime based on globalConfig
		if (CONFIG.isShowDebugLog()) {
			LOGGER.info(msg);
		}
	}

	// ============================================================
	// Singleton-ing
	// ============================================================

	private static MapSyncMod INSTANCE;
	public static @NotNull MapSyncMod getMod() {
		return INSTANCE;
	}

	public MapSyncMod() {
		if (INSTANCE != null) {
			throw new IllegalStateException("Constructor called twice");
		}
		INSTANCE = this;
	}
}
