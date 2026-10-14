package dev.rydonut.settings;

import dev.rydonut.render.Colors;

/**
 * RGBA color setting. The value is stored as ARGB int.
 */
public class ColorSetting extends Setting<Integer> {
	private final boolean allowAlpha;

	public ColorSetting(String name, int argb) {
		this(name, "", argb, true);
	}

	public ColorSetting(String name, String description, int argb, boolean allowAlpha) {
		super(name, description, argb);
		this.allowAlpha = allowAlpha;
	}

	public boolean allowsAlpha() {
		return allowAlpha;
	}

	public float red() {
		return Colors.redf(get());
	}

	public float green() {
		return Colors.greenf(get());
	}

	public float blue() {
		return Colors.bluef(get());
	}

	public float alpha() {
		return Colors.alphaf(get());
	}

	public int withAlpha(int alpha) {
		return Colors.argb(alpha, Colors.red(get()), Colors.green(get()), Colors.blue(get()));
	}

	@Override
	public String serializeValue() {
		return String.valueOf(get());
	}

	@Override
	public void deserializeValue(String raw) {
		try {
			set((int) Long.parseLong(raw.trim()));
		} catch (NumberFormatException ignored) {
		}
	}
}
