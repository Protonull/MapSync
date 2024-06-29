package gjum.minecraft.mapsync.mod;

import gjum.minecraft.mapsync.mod.config.ServerConfig;
import gjum.minecraft.mapsync.mod.network.SyncClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class SyncState {
    private final ClientPacketListener gameConnection;
    public final String gameAddress;
    public final List<SyncClient> syncConnections;
    public final ServerConfig serverConfig;

    public SyncState(
        final @NotNull ClientPacketListener gameConnection
    ) {
        this.gameConnection = Objects.requireNonNull(gameConnection);
        this.gameAddress = gameConnection.getServerData().ip;
        this.syncConnections = new ArrayList<>();
        this.serverConfig = ServerConfig.load(this.gameAddress);
    }

    public synchronized void updateSyncConnections() {
        final List<String> configuredSyncAddresses = new ArrayList<>(this.serverConfig.getSyncServerAddresses());
        final var existingConnections = new HashMap<String, SyncClient>();
        for (final var iter = this.syncConnections.listIterator(); iter.hasNext();) {
            final SyncClient syncConnection = iter.next();
            if (!configuredSyncAddresses.contains(syncConnection.address)) {
                syncConnection.shutDown();
                iter.remove();
                continue;
            }
            existingConnections.put(syncConnection.address, syncConnection);
        }
        configuredSyncAddresses.removeIf(existingConnections::containsKey);
        for (final String syncAddress : configuredSyncAddresses) {
            final var syncConnection = new SyncClient(
                syncAddress,
                this.gameAddress
            );
            syncConnection.autoReconnect = true;
            this.syncConnections.add(syncConnection);
        }
    }

    public synchronized void closeSyncConnections() {
        this.syncConnections.forEach(SyncClient::shutDown);
        this.syncConnections.clear();
    }

    @ApiStatus.Internal
    public synchronized void init() {
        updateSyncConnections();
    }

    @ApiStatus.Internal
    public synchronized void close() {
        closeSyncConnections();
    }

    @ApiStatus.Internal
    public interface Holder {
        @NotNull SyncState getMapSyncState();
    }
}
