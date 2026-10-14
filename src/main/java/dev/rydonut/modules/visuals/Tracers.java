package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.EnumSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Tracers: lines from the camera to tracked entities.
 */
public class Tracers extends Module {
	public enum Target {
		PLAYERS, HOSTILE, PASSIVE, ITEMS, ALL
	}

	private final EnumSetting<Target> target = new EnumSetting<>("Target", Target.PLAYERS);
	private final DoubleSetting range = new DoubleSetting("Range", 96, 16, 256, 8);
	private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
	private final ColorSetting playerColor = new ColorSetting("Player color", 0xFF4FC3F7);
	private final ColorSetting hostileColor = new ColorSetting("Hostile color", 0xFFFF6B6B);
	private final ColorSetting passiveColor = new ColorSetting("Passive color", 0xFF7BE38B);
	private final ColorSetting itemColor = new ColorSetting("Item color", 0xFFFFC04D);

	public Tracers() {
		super("Tracers", Category.VISUALS, "Draws lines to entities.");
		addSettings(target, range, throughWalls, playerColor, hostileColor, passiveColor, itemColor);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			Vec3 eye = mc.gameRenderer.getMainCamera().position();
			double rangeSq = range.get() * range.get();
			Target t = target.get();
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity == mc.player || entity.isSpectator()) {
					continue;
				}
				if (entity.distanceToSqr(mc.player) > rangeSq) {
					continue;
				}
				int color;
				boolean match;
				if (entity instanceof Player) {
					match = t == Target.PLAYERS || t == Target.ALL;
					color = playerColor.get();
				} else if (entity instanceof Monster) {
					match = t == Target.HOSTILE || t == Target.ALL;
					color = hostileColor.get();
				} else if (entity instanceof Animal) {
					match = t == Target.PASSIVE || t == Target.ALL;
					color = passiveColor.get();
				} else if (entity instanceof ItemEntity) {
					match = t == Target.ITEMS || t == Target.ALL;
					color = itemColor.get();
				} else {
					match = false;
					color = Colors.WHITE;
				}
				if (!match) {
					continue;
				}
				float alpha = 1.0f - Mth.clamp((float) (entity.distanceTo(mc.player) / range.get()), 0.15f, 1.0f) * 0.5f;
				WorldShapes.line(eye.x, eye.y, eye.z,
						entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
						Colors.withAlpha(color, alpha), throughWalls.isOn());
			}
		} finally {
			WorldShapes.end();
		}
	}
}
