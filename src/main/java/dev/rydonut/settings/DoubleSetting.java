package dev.rydonut.settings;

public class DoubleSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public DoubleSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, "", clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public DoubleSetting(String name, String description, double defaultValue, double min, double max, double step) {
		super(name, description, clamp(defaultValue, min, max));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	@Override
	public void set(Double value) {
		super.set(clamp(value, min, max));
	}

	public String format() {
		return String.format("%.2f", get());
	}

	@Override
	public String serializeValue() {
		return String.valueOf(get());
	}

	@Override
	public void deserializeValue(String raw) {
		try {
			set(Double.parseDouble(raw.trim()));
		} catch (NumberFormatException ignored) {
		}
	}
}
