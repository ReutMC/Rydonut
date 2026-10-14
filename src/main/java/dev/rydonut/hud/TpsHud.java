package dev.rydonut.hud;

import dev.rydonut.render.Colors;
import dev.rydonut.util.TpsTracker;

/**
 * TPS estimate HUD element (client-side heuristic).
 */
public class TpsHud extends HudModule {
	public TpsHud() {
		super("TPS", "Estimated server TPS.");
		setPosition(6, 92);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		var font = mc.font;
		double tps = TpsTracker.tps();
		String text = "TPS: " + String.format("%.1f", tps);
		int color = tps >= 19.0 ? Colors.SUCCESS : tps >= 15.0 ? Colors.PRIMARY : Colors.DANGER;
		setSize(font.width(text) + 8, font.lineHeight + 6);
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Colors.PANEL);
		graphics.drawString(font, text, getX() + 4, getY() + 3, color);
	}
}
