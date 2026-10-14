package dev.rydonut.util;

import net.minecraft.client.Minecraft;

/**
 * Client-side TPS estimator: tracks server-world time drift.
 * Heuristic only — the client cannot measure server tick rate directly.
 */
public final class TpsTracker {
	private static long lastGameTime = -1;
	private static long lastRealMs = -1;
	private static double smoothedTps = 20.0;

	private TpsTracker() {
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			lastGameTime = -1;
			smoothedTps = 20.0;
			return;
		}
		long gameTime = mc.level.getLevelData().getGameTime();
		long now = System.currentTimeMillis();
		if (lastGameTime >= 0 && gameTime > lastGameTime) {
			long ticks = gameTime - lastGameTime;
			long ms = now - lastRealMs;
			if (ms > 500) {
				double tps = ticks * 1000.0 / ms;
				tps = Math.max(1.0, Math.min(20.0, tps));
				smoothedTps = smoothedTps * 0.8 + tps * 0.2;
				lastGameTime = gameTime;
				lastRealMs = now;
			}
		} else if (lastGameTime < 0) {
			lastGameTime = gameTime;
			lastRealMs = now;
		} else if (gameTime == lastGameTime && now - lastRealMs > 1000) {
			// world time frozen for a second: server is likely lagging hard
			smoothedTps = smoothedTps * 0.9 + 1.0 * 0.1;
		}
	}

	public static double tps() {
		return smoothedTps;
	}

	public static String colorKey() {
		double t = smoothedTps;
		if (t >= 19.0) {
			return "§a";
		}
		if (t >= 15.0) {
			return "§e";
		}
		return "§c";
	}
}
