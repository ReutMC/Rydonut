package dev.rydonut.util;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.rydonut.render.Colors;

/**
 * Simple HUD toast notifications (client-side only).
 */
public final class Notifications {
	public static final class Toast {
		public final String title;
		public final String message;
		public final long createdMs;
		public final long durationMs;

		Toast(String title, String message, long durationMs) {
			this.title = title;
			this.message = message;
			this.createdMs = System.currentTimeMillis();
			this.durationMs = durationMs;
		}

		public float progress() {
			return Math.min(1.0f, (System.currentTimeMillis() - createdMs) / (float) durationMs);
		}

		public boolean expired() {
			return System.currentTimeMillis() - createdMs > durationMs;
		}
	}

	private static final Deque<Toast> TOASTS = new ArrayDeque<>();
	private static long lastToastMs;
	private static long cooldownMs = 3000;

	private Notifications() {
	}

	public static void setCooldownMs(long ms) {
		cooldownMs = Math.max(0, ms);
	}

	public static void push(String title, String message) {
		long now = System.currentTimeMillis();
		if (now - lastToastMs < cooldownMs) {
			return; // cooldown, avoids spam
		}
		lastToastMs = now;
		synchronized (TOASTS) {
			TOASTS.addLast(new Toast(title, message, 5000));
			while (TOASTS.size() > 6) {
				TOASTS.removeFirst();
			}
		}
		// sound cue via verified API
		try {
			var mc = net.minecraft.client.Minecraft.getInstance();
			mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
					net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f));
		} catch (Exception ignored) {
		}
	}

	public static Deque<Toast> current() {
		synchronized (TOASTS) {
			TOASTS.removeIf(Toast::expired);
			return TOASTS;
		}
	}

	public static int accent() {
		return Colors.PRIMARY;
	}
}
