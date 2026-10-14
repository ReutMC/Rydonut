package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.util.TrajectorySim;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Ender pearl / projectile trajectory prediction (visual only, no packets).
 */
public class TrajectoryEsp extends Module {
	private final BoolSetting pearls = new BoolSetting("Ender pearls", true);
	private final BoolSetting arrows = new BoolSetting("Bow/Crossbow", true);
	private final BoolSetting throwables = new BoolSetting("Eggs/Snowballs", true);
	private final BoolSetting showLanding = new BoolSetting("Landing marker", true);
	private final DoubleSetting range = new DoubleSetting("Max distance", 96, 16, 192, 8);

	public TrajectoryEsp() {
		super("Trajectory", Category.VISUALS, "Predicts ender pearl and projectile paths.");
		addSettings(pearls, arrows, throwables, showLanding, range);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		ItemStack main = mc.player.getMainHandItem();
		ItemStack off = mc.player.getOffhandItem();
		ItemStack active = null;
		if (pearls.isOn() && (main.is(Items.ENDER_PEARL) || off.is(Items.ENDER_PEARL))) {
			active = main.is(Items.ENDER_PEARL) ? main : off;
		} else if (arrows.isOn() && (main.is(Items.BOW) || main.is(Items.CROSSBOW))) {
			active = main;
		} else if (throwables.isOn() && (main.is(Items.EGG) || main.is(Items.SNOWBALL) || off.is(Items.EGG) || off.is(Items.SNOWBALL))) {
			active = main.is(Items.EGG) || main.is(Items.SNOWBALL) ? main : off;
		}
		if (active == null) {
			return;
		}
		Vec3 start = new Vec3(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
		Vec3 look = mc.player.getViewVector(1.0f);
		double gravity;
		double drag;
		double speed;
		if (active.is(Items.ENDER_PEARL) || active.is(Items.EGG) || active.is(Items.SNOWBALL)) {
			gravity = TrajectorySim.pearlGravity();
			drag = TrajectorySim.genericDrag();
			speed = 1.5;
		} else {
			gravity = TrajectorySim.arrowGravity();
			drag = TrajectorySim.genericDrag();
			speed = 3.0; // full charge approximation
		}
		Vec3 velocity = look.scale(speed);
		TrajectorySim.Result result = TrajectorySim.simulate(start, velocity, gravity, drag, mc.level, 200);

		WorldShapes.begin(context);
		try {
			int color = Colors.PRIMARY;
			Vec3 prev = start;
			int drawn = 0;
			for (Vec3 point : result.points) {
				if (point.distanceToSqr(start) > range.get() * range.get()) {
					break;
				}
				WorldShapes.line(prev.x, prev.y, prev.z, point.x, point.y, point.z, Colors.withAlpha(color, 0.8f), true);
				prev = point;
				if (++drawn > 400) {
					break;
				}
			}
			if (showLanding.isOn() && result.landing != null) {
				Vec3 land = result.landing;
				WorldShapes.filledBox(land.x - 0.3, land.y - 0.05, land.z - 0.3, land.x + 0.3, land.y + 0.05, land.z + 0.3,
						Colors.withAlpha(Colors.SUCCESS, 0.5f), true);
				WorldShapes.outlinedBox(land.x - 0.3, land.y - 0.05, land.z - 0.3, land.x + 0.3, land.y + 0.05, land.z + 0.3,
						Colors.SUCCESS, true);
			}
		} finally {
			WorldShapes.end();
		}
	}

	@SuppressWarnings("unused")
	private static Item unused() {
		return Items.ENDER_PEARL;
	}
}
