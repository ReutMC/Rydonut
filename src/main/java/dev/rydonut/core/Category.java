package dev.rydonut.core;

/**
 * Module categories shown as panels in the ClickGUI.
 */
public enum Category {
	VISUALS("Visuals"),
	DETECTION("Detection"),
	HUD("HUD"),
	MISC("Misc");

	public final String title;

	Category(String title) {
		this.title = title;
	}
}
