package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Nametags: scaled, readable name tags for players (and optionally mobs) with
 * health, held item and distance.
 */
public class Nametags extends Module {
	private final DoubleSetting range = new DoubleSetting("Range", 64, 8, 128, 8);
	private final DoubleSetting scale = new DoubleSetting("Scale", 1.2, 0.5, 3, 0.1);
	private final BoolSetting players = new BoolSetting("Players", true);
	private final BoolSetting mobs = new BoolSetting("Mobs", false);
	private final BoolSetting showHealth = new BoolSetting("Health", true);
	private final BoolSetting showHeldItem = new BoolSetting("Held item", true);
	private final BoolSetting showDistance = new BoolSetting("Distance", true);
	private final BoolSetting alwaysVisible = new BoolSetting("Through walls", true);
	private final BoolSetting background = new BoolSetting("Background", true);

	public Nametags() {
		super("Nametags", Category.VISUALS, "Scaled readable name tags with health and gear.");
		addSettings(range, scale, players, mobs, showHealth, showHeldItem, showDistance, alwaysVisible, background);
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
				if (entity == mc.player || !(entity instanceof LivingEntity living) || entity.isSpectator()) {
					continue;
				}
				boolean isPlayer = entity instanceof Player;
				if (isPlayer ? !players.isOn() : !mobs.isOn()) {
					continue;
				}
				double distSq = entity.distanceToSqr(mc.player);
				if (distSq > rangeSq) {
					continue;
				}
				double dist = Math.sqrt(distSq);
				StringBuilder sb = new StringBuilder();
				sb.append(entity.getName().getString());
				if (showHealth.isOn()) {
					float hp = Math.max(0.0f, living.getHealth());
					sb.append(" §c").append(String.format("%.1f", hp));
				}
				if (showDistance.isOn()) {
					sb.append(" §7[").append((int) dist).append("m]");
				}
				int color = isPlayer ? Colors.healthColor(living.getHealth() / Math.max(1.0f, living.getMaxHealth())) : Colors.WHITE;
				float s = (float) (scale.get() * Mth.clamp(dist / 16.0, 0.7, 3.0));
				int bg = background.isOn() ? 0xA0000000 : 0;
				WorldShapes.text(sb.toString(), entity.getX(), entity.getBoundingBox().maxY + 0.3, entity.getZ(),
						s, color, bg, alwaysVisible.isOn());
				if (showHeldItem.isOn()) {
					ItemStackLike held = new ItemStackLike(living.getMainHandItem());
					if (!held.isEmpty()) {
						WorldShapes.text(held.name(), entity.getX(), entity.getBoundingBox().maxY + 0.05, entity.getZ(),
								s * 0.85f, Colors.TEXT_DIM, bg, alwaysVisible.isOn());
					}
				}
			}
		} finally {
			WorldShapes.end();
		}
	}

	private record ItemStackLike(net.minecraft.world.item.ItemStack stack) {
		ItemStackLike {
		}

		boolean isEmpty() {
			return stack == null || stack.isEmpty();
		}

		String name() {
			return stack.getHoverName().getString();
		}
	}
}
