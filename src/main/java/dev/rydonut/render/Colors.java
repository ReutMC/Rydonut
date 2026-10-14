package dev.rydonut.render;

import net.minecraft.util.ARGB;

/**
 * ARGB color helpers + Rydonut theme palette.
 * Theme: light blue on soft dark-blue (#0D1B2A) panels.
 */
public final class Colors {
	public static final int PRIMARY = 0xFF4FC3F7;
	public static final int ACCENT = 0xFF81D4FA;
	public static final int PANEL = 0xE60D1B2A;
	public static final int PANEL_LIGHT = 0xF0162740;
	public static final int OUTLINE = 0x4C4FC3F7;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int TEXT_DIM = 0xFFB9C8D6;
	public static final int DANGER = 0xFFFF6B6B;
	public static final int SUCCESS = 0xFF7BE38B;

	private Colors() {
	}

	public static int argb(int a, int r, int g, int b) {
		return ARGB.color(a, r, g, b);
	}

	public static int red(int argb) {
		return (argb >> 16) & 0xFF;
	}

	public static int green(int argb) {
		return (argb >> 8) & 0xFF;
	}

	public static int blue(int argb) {
		return argb & 0xFF;
	}

	public static int alpha(int argb) {
		return (argb >>> 24) & 0xFF;
	}

	public static float redf(int argb) {
		return red(argb) / 255.0f;
	}

	public static float greenf(int argb) {
		return green(argb) / 255.0f;
	}

	public static float bluef(int argb) {
		return blue(argb) / 255.0f;
	}

	public static float alphaf(int argb) {
		return alpha(argb) / 255.0f;
	}

	/** Multiplies the alpha channel of an ARGB color by a factor. */
	public static int withAlpha(int argb, float alphaFactor) {
		int a = (int) (alpha(argb) * Math.max(0.0f, Math.min(1.0f, alphaFactor)));
		return argb(a, red(argb), green(argb), blue(argb));
	}

	/** Interpolates between two ARGB colors (channel-wise). */
	public static int lerp(int from, int to, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int a = Math.round(alpha(from) + (alpha(to) - alpha(from)) * t);
		int r = Math.round(red(from) + (red(to) - red(from)) * t);
		int g = Math.round(green(from) + (green(to) - green(from)) * t);
		int b = Math.round(blue(from) + (blue(to) - blue(from)) * t);
		return argb(a, r, g, b);
	}

	/** Health (0..1) -> color from red to green. */
	public static int healthColor(float health01) {
		health01 = Math.max(0.0f, Math.min(1.0f, health01));
		int r = (int) (255 * (1 - health01));
		int g = (int) (255 * health01);
		return argb(255, r, g, 60);
	}
}
