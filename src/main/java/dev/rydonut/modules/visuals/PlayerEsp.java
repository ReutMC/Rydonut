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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Player ESP: box / outline / 2D / filled modes, health- or distance-based
 * colors, optional fill and tracers. Purely visual, client-side only.
 */
public class PlayerEsp extends Module {
	public enum Mode {
		BOX, OUTLINE, TWO_D, FILLED
	}

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode", Mode.BOX);
	private final DoubleSetting range = new DoubleSetting("Range", 64, 8, 256, 8);
	private final BoolSetting healthColor = new BoolSetting("Health color", true);
	private final BoolSetting distanceColor = new BoolSetting("Distance color", false);
	private final BoolSetting showFill = new BoolSetting("Fill", true);
	private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.25, 0.05, 1.0, 0.05);
	private final DoubleSetting lineWidth = new DoubleSetting("Line width", 1, 0.5, 4, 0.5);
	private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
	private final BoolSetting tracers = new BoolSetting("Tracers", false);
	private final ColorSetting customColor = new ColorSetting("Custom color", 0xFF4FC3F7);

	public PlayerEsp() {
		super("Player ESP", Category.VISUALS, "Highlights other players.");
		addSettings(mode, range, healthColor, distanceColor, showFill, fillOpacity, lineWidth, throughWalls, tracers, customColor);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		double rangeSq = range.get() * range.get();
		try {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity == mc.player || entity.getType() != EntityType.PLAYER) {
					continue;
				}
				double distSq = entity.distanceToSqr(mc.player);
				if (distSq > rangeSq) {
					continue;
				}
				AABB box = entity.getBoundingBox();
				int color = colorFor(entity, distSq);
				renderBox(box, color);
				if (tracers.isOn()) {
					tracer(entity, color);
				}
			}
		} finally {
			WorldShapes.end();
		}
	}

	private int colorFor(Entity entity, double distSq) {
		if (distanceColor.isOn()) {
			float t = Mth.clamp((float) (Math.sqrt(distSq) / range.get()), 0.0f, 1.0f);
			return Colors.lerp(Colors.PRIMARY, Colors.DANGER, t);
		}
		if (healthColor.isOn() && entity instanceof LivingEntity living) {
			return Colors.healthColor(living.getHealth() / Math.max(1.0f, living.getMaxHealth()));
		}
		return customColor.get();
	}

	private void renderBox(AABB box, int color) {
		Mode m = mode.get();
		boolean tw = throughWalls.isOn();
		if (showFill.isOn() && (m == Mode.BOX || m == Mode.FILLED)) {
			WorldShapes.filledBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
					Colors.withAlpha(color, fillOpacity.get().floatValue()), tw);
		}
		switch (m) {
			case BOX, FILLED -> WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, tw);
			case OUTLINE -> {
				// brighter single outline pass
				WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, tw);
				WorldShapes.outlinedBox(box.minX - 0.02, box.minY - 0.02, box.minZ - 0.02,
						box.maxX + 0.02, box.maxY + 0.02, box.maxZ + 0.02, Colors.withAlpha(color, 0.6f), tw);
			}
			case TWO_D -> {
				double cx = (box.minX + box.maxX) / 2.0;
				double cz = (box.minZ + box.maxZ) / 2.0;
				double r = Math.max(box.maxX - box.minX, box.maxZ - box.minZ) / 2.0 + 0.05;
				WorldShapes.outlinedBox(cx - r, box.minY, cz - r, cx + r, box.minY + 0.02, cz + r, color, tw);
				WorldShapes.outlinedBox(cx - r, box.maxY, cz - r, cx + r, box.maxY + 0.02, cz + r, color, tw);
			}
		}
	}

	private void tracer(Entity entity, int color) {
		Vec3Like eye = new Vec3Like(mc.gameRenderer.getMainCamera().position());
		WorldShapes.line(eye.x, eye.y, eye.z, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
				color, throughWalls.isOn());
	}

	private record Vec3Like(double x, double y, double z) {
		Vec3Like(net.minecraft.world.phys.Vec3 v) {
			this(v.x, v.y, v.z);
		}
	}
}
