package dev.rydonut.hud;

import dev.rydonut.render.Colors;

/**
 * Ping HUD element (own latency from the player list).
 */
public class PingHud extends HudModule {
	public PingHud() {
		super("Ping", "Own latency.");
		setPosition(6, 42);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		var font = mc.font;
		int ping = getPing();
		String text = ping + " ms";
		int color = ping < 80 ? Colors.SUCCESS : ping < 150 ? Colors.PRIMARY : Colors.DANGER;
		setSize(font.width(text) + 8, font.lineHeight + 6);
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Colors.PANEL);
		graphics.drawString(font, text, getX() + 4, getY() + 3, color);
	}

	private int getPing() {
		try {
			if (mc.getConnection() != null && mc.player != null) {
				var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
				if (info != null) {
					return info.getLatency();
				}
			}
		} catch (Exception ignored) {
		}
		return 0;
	}
}
