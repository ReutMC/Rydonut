package dev.rydonut.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for all module settings.
 */
public abstract class Setting<T> {
	private final String name;
	private final String description;
	private final List<Consumer<T>> changeListeners = new ArrayList<>();
	protected T value;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
		for (Consumer<T> listener : changeListeners) {
			listener.accept(value);
		}
	}

	public void onChange(Consumer<T> listener) {
		changeListeners.add(listener);
	}

	/** Whether the setting should currently be visible in the GUI. */
	public boolean isVisible() {
		return true;
	}

	/** Serializable representation used by the config system. */
	public abstract String serializeValue();

	public abstract void deserializeValue(String raw);
}
