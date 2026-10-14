package dev.rydonut.hud;

import dev.rydonut.render.Colors;
import dev.rydonut.util.FpsTracker;

/**
 * FPS HUD element.
 */
public class FpsHud extends HudModule {
	public FpsHud() {
		super("FPS", "Frames per second.");
		setPosition(6, 24);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		var font = mc.font;
		int fps = (int) FpsTracker.fps();
		String text = fps + " FPS";
		int color = fps >= 100 ? Colors.SUCCESS : fps >= 60 ? Colors.PRIMARY : Colors.DANGER;
		setSize(font.width(text) + 8, font.lineHeight + 6);
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Colors.PANEL);
		graphics.drawString(font, text, getX() + 4, getY() + 3, color);
	}
}
