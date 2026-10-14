package dev.rydonut.hud;

import dev.rydonut.core.Category;
import dev.rydonut.render.Colors;

/**
 * "Rydonut" watermark with version and user.
 */
public class WatermarkHud extends HudModule {
	public WatermarkHud() {
		super("Watermark", "Rydonut branding.");
		setPosition(6, 6);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		var font = mc.font;
		String text = "Rydonut §7v0.1.0";
		int width = font.width("Rydonut v0.1.0") + 8;
		setSize(width, font.lineHeight + 6);
		graphics.fill(getX(), getY(), getX() + width, getY() + getHeight(), Colors.PANEL);
		graphics.fill(getX(), getY(), getX() + 2, getY() + getHeight(), Colors.PRIMARY);
		graphics.drawString(font, text, getX() + 6, getY() + 3, Colors.PRIMARY);
	}
}
