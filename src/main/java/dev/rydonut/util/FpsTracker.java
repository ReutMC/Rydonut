package dev.rydonut.util;

/**
 * Simple FPS counter (frames per second, updated 4x per second).
 */
public final class FpsTracker {
	private static int frames;
	private static long windowStart = System.currentTimeMillis();
	private static float fps = 0.0f;

	private FpsTracker() {
	}

	public static void frame() {
		frames++;
		long now = System.currentTimeMillis();
		long elapsed = now - windowStart;
		if (elapsed >= 250) {
			fps = frames * 1000.0f / elapsed;
			frames = 0;
			windowStart = now;
		}
	}

	public static float fps() {
		return fps;
	}
}
