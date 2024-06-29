package gjum.minecraft.mapsync.mod;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ModGui extends Screen {
	final Screen parentScreen;

	private final SyncState syncState = MapSyncMod.getMod().getSyncState().orElseThrow();

	int innerWidth = 300;
	int left;
	int right;
	int top;

	EditBox syncServerAddressField;
	Button syncServerConnectBtn;

	public ModGui(Screen parentScreen) {
		super(Component.literal("Map-Sync"));
		this.parentScreen = parentScreen;
	}

	@Override
	public void resize(Minecraft mc, int width, int height) {
		super.resize(mc, width, height);
		init();
	}

	@Override
	protected void init() {
		try {
			left = width / 2 - innerWidth / 2;
			right = width / 2 + innerWidth / 2;
			top = height / 3;

			clearWidgets();

			addRenderableWidget(
				Button.builder(Component.literal("Close"), (button) -> this.minecraft.setScreen(this.parentScreen))
					.pos(this.right - 100, this.top)
					.width(100)
					.build()
			);

			addWidget(syncServerAddressField = new EditBox(font,
					left,
					top + 40,
					innerWidth - 110, 20,
					Component.literal("Sync Server Address")));
			syncServerAddressField.setMaxLength(256);
			synchronized (this.syncState) {
				this.syncServerAddressField.setValue(String.join(" ", this.syncState.serverConfig.getSyncServerAddresses()));
			}

			addRenderableWidget(
				syncServerConnectBtn = Button.builder(Component.literal("Connect"), this::connectClicked)
					.pos(this.right - 100, this.top + 40)
					.width(100)
					.build()
			);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void connectClicked(Button btn) {
		try {
			if (syncServerAddressField == null) return;
			var addresses = List.of(syncServerAddressField.getValue().split("[^-_.:A-Za-z0-9]+"));
			synchronized (this.syncState) {
				this.syncState.serverConfig.setSyncServerAddresses(addresses);
				this.syncState.updateSyncConnections();
			}
			btn.active = false;
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(
		final @NotNull GuiGraphics guiGraphics,
		final int mouseX,
		final int mouseY,
		final float partialTick
	) {
		try {
			// wait for init() to finish
			if (syncServerAddressField == null) return;
			if (syncServerConnectBtn == null) return;

			renderBackground(guiGraphics, mouseX, mouseY, partialTick);
			guiGraphics.drawCenteredString(this.font, title, this.width / 2, this.top, 0xFFFFFF);
			this.syncServerAddressField.render(guiGraphics, mouseX, mouseY, partialTick);

			var dimensionState = MapSyncMod.getMod().getDimensionState();
			if (dimensionState != null) {
				String counterText = String.format(
						"In dimension %s, received %d chunks, rendered %d, rendering %d",
						dimensionState.dimension.location(),
						dimensionState.getNumChunksReceived(),
						dimensionState.getNumChunksRendered(),
						dimensionState.getRenderQueueSize()
				);
				guiGraphics.drawString(this.font, counterText, this.left, this.top + 70, 0x888888);
			}

			int numConnected = 0;
			int msgY = top + 90;
			var syncClients = MapSyncMod.getMod().getSyncClients();
			for (var client : syncClients) {
				int statusColor;
				String statusText;
				if (client.isEncrypted()) {
					numConnected++;
					statusColor = 0x008800;
					statusText = "Connected";
				} else if (client.getError() != null) {
					statusColor = 0xff8888;
					statusText = client.getError();
				} else {
					statusColor = 0xffffff;
					statusText = "Connecting...";
				}
				statusText = client.address + "  " + statusText;
				guiGraphics.drawString(this.font, statusText, this.left, msgY, statusColor);
				msgY += 10;
			}

			super.render(guiGraphics, mouseX, mouseY, partialTick);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
