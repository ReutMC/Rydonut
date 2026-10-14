package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Chams: renders entity bounding outlines through walls.
 *
 * <p>True shader-based chams (re-rendering entity models with depth testing
 * disabled) is not safely implementable with public 1.21.11 APIs without
 * deeper render-state mixins, so this module uses the verified custom
 * no-depth-test pipeline to draw through-wall outlines of entities instead.
 */
public class Chams extends Module {
	private final DoubleSetting range = new DoubleSetting("Range", 64, 8, 128, 8);
	private final BoolSetting players = new BoolSetting("Players", true);
	private final BoolSetting mobs = new BoolSetting("Mobs", true);
	private final BoolSetting showFill = new BoolSetting("Fill", true);
	private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.12, 0.02, 0.5, 0.02);
	private final ColorSetting color = new ColorSetting("Color", 0xFF81D4FA);

	public Chams() {
		super("Chams", Category.VISUALS, "See-through entity outlines (through-wall box chams).");
		addSettings(range, players, mobs, showFill, fillOpacity, color);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			double rangeSq = range.get() * range.get();
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity == mc.player || entity.isSpectator()) {
					continue;
				}
				boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
				if (!isPlayer && !(mobs.isOn() && entity instanceof LivingEntity)) {
					continue;
				}
				if (isPlayer && !players.isOn()) {
					continue;
				}
				if (entity.distanceToSqr(mc.player) > rangeSq) {
					continue;
				}
				AABB box = entity.getBoundingBox();
				int c = color.get();
				if (showFill.isOn()) {
					WorldShapes.filledBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
							Colors.withAlpha(c, fillOpacity.get().floatValue()), true);
				}
				WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, c, true);
			}
		} finally {
			WorldShapes.end();
		}
	}
}
