package dev.rydonut.settings;

public class IntSetting extends Setting<Integer> {
	private final int min;
	private final int max;
	private final int step;

	public IntSetting(String name, int defaultValue, int min, int max, int step) {
		super(name, "", clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public IntSetting(String name, String description, int defaultValue, int min, int max, int step) {
		super(name, description, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public int getStep() {
		return step;
	}

	@Override
	public void set(Integer value) {
		super.set(clamp(value, min, max));
	}

	@Override
	public String serializeValue() {
		return String.valueOf(get());
	}

	@Override
	public void deserializeValue(String raw) {
		try {
			set(Integer.parseInt(raw.trim()));
		} catch (NumberFormatException ignored) {
		}
	}
}
