package dev.rydonut.settings;

public class StringSetting extends Setting<String> {
	public StringSetting(String name, String defaultValue) {
		super(name, "", defaultValue);
	}

	public StringSetting(String name, String description, String defaultValue) {
		super(name, description, defaultValue);
	}

	@Override
	public String serializeValue() {
		return get() == null ? "" : get();
	}

	@Override
	public void deserializeValue(String raw) {
		set(raw == null ? "" : raw);
	}
}
