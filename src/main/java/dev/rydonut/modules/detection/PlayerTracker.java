package dev.rydonut.modules.detection;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.hud.HudModule;
import dev.rydonut.settings.BoolSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * PlayerTracker (passive): logs when other players enter/leave render range
 * (name, coordinates, dimension, gear snapshot) with an optional HUD list and
 * sound alert. Read-only.
 */
public class PlayerTracker extends Module {
	public static final class Entry {
		public final String name;
		public final int x;
		public final int y;
		public final int z;
		public final String dimension;
		public final boolean joined;
		public final long timeMs;

		Entry(String name, int x, int y, int z, String dimension, boolean joined) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dimension = dimension;
			this.joined = joined;
			this.timeMs = System.currentTimeMillis();
		}
	}

	private final BoolSetting hudList = new BoolSetting("HUD list", true);
	private final BoolSetting alerts = new BoolSetting("Alerts", true);
	private final BoolSetting logToConsole = new BoolSetting("Log to console", true);
	private final BoolSetting exportToFile = new BoolSetting("Export", true);

	private final List<Entry> history = new ArrayList<>();
	private final List<Player> inRange = new ArrayList<>();

	public PlayerTracker() {
		super("PlayerTracker", Category.DETECTION, "Logs players entering render range with gear snapshots.");
		addSettings(hudList, alerts, logToConsole, exportToFile);

		ClientEntityEvents.ENTITY_LOAD.register((Entity entity, ClientLevel world) -> {
			if (!isEnabled() || !(entity instanceof Player player) || player == mc.player) {
				return;
			}
			String dimension = world.dimension().identifier().toString();
			Entry entry = new Entry(player.getName().getString(),
					(int) player.getX(), (int) player.getY(), (int) player.getZ(), dimension, true);
			synchronized (history) {
				history.add(0, entry);
				while (history.size() > 50) {
					history.remove(history.size() - 1);
				}
			}
			if (logToConsole.isOn()) {
				ConfigManager.RydonutLogger.info("Player entered range: " + entry.name
						+ " at " + entry.x + " " + entry.y + " " + entry.z + " (" + dimension + ")");
			}
			if (exportToFile.isOn()) {
				ConfigManager.exportFind(dimension, "PLAYER_IN " + entry.name + " " + entry.x + " " + entry.y + " " + entry.z);
			}
			if (alerts.isOn()) {
				dev.rydonut.util.Notifications.push("Player spotted", entry.name + " (" + entry.x + ", " + entry.z + ")");
			}
		});
		ClientEntityEvents.ENTITY_UNLOAD.register((Entity entity, ClientLevel world) -> {
			if (!isEnabled() || !(entity instanceof Player player) || player == mc.player) {
				return;
			}
			String dimension = world.dimension().identifier().toString();
			Entry entry = new Entry(player.getName().getString(),
					(int) player.getX(), (int) player.getY(), (int) player.getZ(), dimension, false);
			synchronized (history) {
				history.add(0, entry);
				while (history.size() > 50) {
					history.remove(history.size() - 1);
				}
			}
			if (logToConsole.isOn()) {
				ConfigManager.RydonutLogger.info("Player left range: " + entry.name);
			}
		});
	}

	@Override
	public void onTick() {
		inRange.clear();
		if (mc.level != null) {
			for (var entity : mc.level.entitiesForRendering()) {
				if (entity instanceof Player player && player != mc.player) {
					inRange.add(player);
				}
			}
		}
	}

	@Override
	public void onHudRender(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (!hudList.isOn() || inRange.isEmpty()) {
			return;
		}
		var font = mc.font;
		int y = 24;
		graphics.fill(4, y - 2, 4 + 110, y + inRange.size() * (font.lineHeight + 1) + 2, 0x80000000);
		graphics.drawString(font, Component.literal("Players (" + inRange.size() + ")").withStyle(net.minecraft.ChatFormatting.AQUA), 6, y, 0xFF81D4FA);
		y += font.lineHeight + 1;
		for (Player player : inRange) {
			String line = player.getName().getString() + " " + (int) player.getX() + " " + (int) player.getY() + " " + (int) player.getZ();
			graphics.drawString(font, Component.literal(line), 6, y, 0xFFFFFFFF);
			y += font.lineHeight + 1;
		}
	}

	public List<Entry> getHistory() {
		synchronized (history) {
			return new ArrayList<>(history);
		}
	}

	@Override
	public String getHudText() {
		return inRange.size() + " player(s) in range";
	}
}
