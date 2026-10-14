package dev.rydonut.hud;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;

/**
 * Base class for draggable HUD elements. Positions are in scaled-GUI pixels
 * and persisted by the config system.
 */
public abstract class HudModule extends Module {
	private int x = 5;
	private int y = 5;
	private int width = 60;
	private int height = 12;

	protected HudModule(String name, String description) {
		super(name, Category.HUD, description);
		addSettings(new IntSettingAdapter());
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		ConfigManager.markDirty();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setPosition(int x, int y) {
		this.x = Math.max(0, x);
		this.y = Math.max(0, y);
		ConfigManager.markDirty();
	}

	public void setSize(int width, int height) {
		this.width = Math.max(10, width);
		this.height = Math.max(6, height);
	}

	/**
	 * Internal: width/height are runtime-measured, but they must survive config
	 * round-trips; we piggyback on the settings list with hidden settings.
	 */
	private class IntSettingAdapter extends dev.rydonut.settings.Setting<Integer> {
		IntSettingAdapter() {
			super("#pos", "", 0);
		}

		@Override
		public String serializeValue() {
			return x + "," + y;
		}

		@Override
		public void deserializeValue(String raw) {
			try {
				String[] parts = raw.split(",");
				setPosition(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
			} catch (Exception ignored) {
			}
		}
	}
}
