package dev.rydonut.modules.detection;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.IntSetting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * LogoutSpot ESP (passive): remembers the last known position of players when
 * they leave render range and shows a fading ghost box for N minutes.
 */
public class LogoutSpots extends Module {
	public static final class Spot {
		public final String name;
		public final double x;
		public final double y;
		public final double z;
		public final String dimension;
		public final long timeMs;

		Spot(String name, double x, double y, double z, String dimension) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.dimension = dimension;
			this.timeMs = System.currentTimeMillis();
		}

		public boolean expired(long timeoutMs) {
			return System.currentTimeMillis() - timeMs > timeoutMs;
		}
	}

	private final IntSetting timeoutMinutes = new IntSetting("Keep for (min)", 5, 1, 60, 1);
	private final BoolSetting showName = new BoolSetting("Show name", true);

	private final Map<String, Spot> spots = new ConcurrentHashMap<>();

	public LogoutSpots() {
		super("LogoutSpots", Category.DETECTION, "Ghost boxes where players left render range.");
		addSettings(timeoutMinutes, showName);

		ClientEntityEvents.ENTITY_UNLOAD.register((Entity entity, ClientLevel world) -> {
			if (!isEnabled() || !(entity instanceof Player player) || player == mc.player) {
				return;
			}
			spots.put(player.getName().getString().toLowerCase(),
					new Spot(player.getName().getString(), player.getX(), player.getY(), player.getZ(),
							world.dimension().identifier().toString()));
		});
		ClientEntityEvents.ENTITY_LOAD.register((Entity entity, ClientLevel world) -> {
			if (entity instanceof Player player) {
				spots.remove(player.getName().getString().toLowerCase());
			}
		});
	}

	@Override
	public void onTick() {
		long timeout = timeoutMinutes.get() * 60_000L;
		spots.values().removeIf(spot -> spot.expired(timeout));
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.level == null) {
			return;
		}
		String dimension = mc.level.dimension().identifier().toString();
		WorldShapes.begin(context);
		try {
			Iterator<Map.Entry<String, Spot>> it = spots.entrySet().iterator();
			while (it.hasNext()) {
				Spot spot = it.next().getValue();
				if (!spot.dimension.equals(dimension)) {
					continue;
				}
				long timeout = timeoutMinutes.get() * 60_000L;
				float remain = 1.0f - (System.currentTimeMillis() - spot.timeMs) / (float) timeout;
				int color = Colors.withAlpha(Colors.DANGER, Math.max(0.15f, remain));
				WorldShapes.filledBox(spot.x - 0.3, spot.y, spot.z - 0.3, spot.x + 0.3, spot.y + 1.8, spot.z + 0.3,
						Colors.withAlpha(color, 0.2f), true);
				WorldShapes.outlinedBox(spot.x - 0.3, spot.y, spot.z - 0.3, spot.x + 0.3, spot.y + 1.8, spot.z + 0.3, color, true);
				if (showName.isOn()) {
					WorldShapes.text("Logout: " + spot.name, spot.x, spot.y + 2.1, spot.z, 0.9f, Colors.WHITE, 0x90000000, true);
				}
			}
		} finally {
			WorldShapes.end();
		}
	}

	public Map<String, Spot> getSpots() {
		return spots;
	}
}
