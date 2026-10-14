package dev.rydonut.settings;

/**
 * Keybind setting. Value is a GLFW key code, 0 = unbound.
 */
public class KeybindSetting extends Setting<Integer> {
	private boolean listening;

	public KeybindSetting(String name, int keyCode) {
		super(name, "", keyCode);
	}

	public boolean isBound() {
		return get() != 0;
	}

	public boolean isListening() {
		return listening;
	}

	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public String describe() {
		if (get() == 0) {
			return "None";
		}
		return org.lwjgl.glfw.GLFW.glfwGetKeyName(get(), 0) != null
				? org.lwjgl.glfw.GLFW.glfwGetKeyName(get(), 0).toUpperCase()
				: "Key " + get();
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
