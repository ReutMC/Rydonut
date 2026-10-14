package dev.rydonut.settings;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
	private final Class<E> type;

	public EnumSetting(String name, E defaultValue) {
		super(name, "", defaultValue);
		this.type = defaultValue.getDeclaringClass();
	}

	public EnumSetting(String name, String description, E defaultValue) {
		super(name, description, defaultValue);
		this.type = defaultValue.getDeclaringClass();
	}

	public E[] values() {
		return type.getEnumConstants();
	}

	public void cycle() {
		E[] all = values();
		set(all[(get().ordinal() + 1) % all.length]);
	}

	@Override
	public String serializeValue() {
		return get().name();
	}

	@Override
	public void deserializeValue(String raw) {
		String trimmed = raw.trim();
		for (E e : values()) {
			if (e.name().equalsIgnoreCase(trimmed)) {
				set(e);
				return;
			}
		}
	}
}
