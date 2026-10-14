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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

/**
 * Entity ESP: mobs with hostile/passive/projectile/item-frame filters.
 */
public class EntityEsp extends Module {
	private final DoubleSetting range = new DoubleSetting("Range", 64, 8, 128, 8);
	private final BoolSetting hostile = new BoolSetting("Hostile", true);
	private final BoolSetting passive = new BoolSetting("Passive", true);
	private final BoolSetting projectiles = new BoolSetting("Projectiles", true);
	private final BoolSetting itemFrames = new BoolSetting("Item frames", false);
	private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
	private final BoolSetting showFill = new BoolSetting("Fill", true);
	private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.2, 0.05, 1.0, 0.05);
	private final ColorSetting hostileColor = new ColorSetting("Hostile color", 0xFFFF6B6B);
	private final ColorSetting passiveColor = new ColorSetting("Passive color", 0xFF7BE38B);
	private final ColorSetting projectileColor = new ColorSetting("Projectile color", 0xFFFFC04D);
	private final ColorSetting frameColor = new ColorSetting("Frame color", 0xFFB072E0);

	public EntityEsp() {
		super("Entity ESP", Category.VISUALS, "Highlights mobs and projectiles.");
		addSettings(range, hostile, passive, projectiles, itemFrames, throughWalls, showFill, fillOpacity,
				hostileColor, passiveColor, projectileColor, frameColor);
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
				if (entity == mc.player || entity instanceof ItemEntity) {
					continue;
				}
				if (entity.distanceToSqr(mc.player) > rangeSq) {
					continue;
				}
				int color;
				if (entity instanceof Monster) {
					if (!hostile.isOn()) continue;
					color = hostileColor.get();
				} else if (entity instanceof Animal || entity.getType().getCategory().isFriendly()) {
					if (!passive.isOn()) continue;
					color = passiveColor.get();
				} else if (entity instanceof Projectile) {
					if (!projectiles.isOn()) continue;
					color = projectileColor.get();
				} else if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame
						|| entity instanceof net.minecraft.world.entity.decoration.GlowItemFrame) {
					if (!itemFrames.isOn()) continue;
					color = frameColor.get();
				} else {
					continue;
				}
				AABB box = entity.getBoundingBox();
				boolean tw = throughWalls.isOn();
				if (showFill.isOn()) {
					WorldShapes.filledBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
							Colors.withAlpha(color, fillOpacity.get().floatValue()), tw);
				}
				WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, tw);
			}
		} finally {
			WorldShapes.end();
		}
	}
}
