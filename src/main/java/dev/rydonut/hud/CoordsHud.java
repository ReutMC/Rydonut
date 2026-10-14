package dev.rydonut.hud;

import dev.rydonut.render.Colors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Coordinates HUD with overworld <-> nether coordinate conversion.
 */
public class CoordsHud extends HudModule {
	public CoordsHud() {
		super("Coordinates", "XYZ + nether/overworld converter.");
		setPosition(6, 60);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		var font = mc.font;
		Vec3 pos = mc.player.position();
		boolean nether = mc.level.dimensionType().hasCeiling();
		int ox = (int) Math.floor(pos.x);
		int oy = (int) Math.floor(pos.y);
		int oz = (int) Math.floor(pos.z);
		int cx;
		int cy;
		int cz;
		if (nether) {
			cx = ox * 8;
			cy = oy;
			cz = oz * 8;
		} else {
			cx = ox / 8;
			cy = oy;
			cz = oz / 8;
		}
		String dimLabel = nether ? "Nether" : mc.level.dimension().identifier().getPath().contains("end") ? "End" : "Overworld";
		String line1 = "XYZ: " + ox + " / " + oy + " / " + oz + " §7(" + dimLabel + ")";
		String line2 = (nether ? "§aOverworld: " : "§bNether: ") + cx + " / " + cy + " / " + cz;

		setSize(Math.max(font.width(line1), font.width(line2)) + 8, (font.lineHeight + 2) * 2 + 6);
		graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), Colors.PANEL);
		graphics.drawString(font, Component.literal(line1), getX() + 4, getY() + 3, 0xFFFFFFFF);
		graphics.drawString(font, Component.literal(line2), getX() + 4, getY() + 3 + font.lineHeight + 2, 0xFFB9C8D6);
	}
}
