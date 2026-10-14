package dev.rydonut.settings;

public class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String name, boolean defaultValue) {
		super(name, "", defaultValue);
	}

	public BoolSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean isOn() {
		return get();
	}

	@Override
	public String serializeValue() {
		return String.valueOf(get());
	}

	@Override
	public void deserializeValue(String raw) {
		set(Boolean.parseBoolean(raw.trim()));
	}
}
