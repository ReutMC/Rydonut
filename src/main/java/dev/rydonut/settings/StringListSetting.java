package dev.rydonut.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A user editable list of strings (used for block/item id lists).
 */
public class StringListSetting extends Setting<List<String>> {
	public StringListSetting(String name, List<String> defaultValue) {
		super(name, "", new ArrayList<>(defaultValue));
	}

	public StringListSetting(String name, String description, List<String> defaultValue) {
		super(name, description, new ArrayList<>(defaultValue));
	}

	public boolean contains(String entry) {
		return get().contains(entry);
	}

	public boolean isEmptyList() {
		return get().isEmpty();
	}

	public void add(String entry) {
		String e = entry.toLowerCase();
		if (!get().contains(e)) {
			List<String> copy = new ArrayList<>(get());
			copy.add(e);
			set(copy);
		}
	}

	public void remove(String entry) {
		List<String> copy = new ArrayList<>(get());
		copy.remove(entry.toLowerCase());
		set(copy);
	}

	public void toggle(String entry) {
		if (contains(entry)) {
			remove(entry);
		} else {
			add(entry);
		}
	}

	@Override
	public String serializeValue() {
		StringJoiner joiner = new StringJoiner(",");
		for (String s : get()) {
			joiner.add(s);
		}
		return joiner.toString();
	}

	@Override
	public void deserializeValue(String raw) {
		List<String> list = new ArrayList<>();
		if (raw != null && !raw.isBlank()) {
			for (String part : raw.split(",")) {
				String p = part.trim().toLowerCase();
				if (!p.isEmpty()) {
					list.add(p);
				}
			}
		}
		set(list);
	}
}
