package dev.rydonut.modules.detection;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

/**
 * Stronghold-line helper (visual only): records ender eye throw positions and
 * directions, computes the least-squares intersection of the 2D sight lines
 * and draws them. No packets involved; the eyes are simply observed.
 */
public class EyeOfEnderHelper extends Module {
	private final BoolSetting drawLines = new BoolSetting("Draw lines", true);
	private final BoolSetting showPrediction = new BoolSetting("Show intersection", true);
	private final BoolSetting autoRecord = new BoolSetting("Auto record throws", true);

	private static final class Throw {
		final Vec3 start;
		final Vec3 direction;
		final long timeMs;

		Throw(Vec3 start, Vec3 direction) {
			this.start = start;
			this.direction = direction;
			this.timeMs = System.currentTimeMillis();
		}
	}

	private final List<Throw> throwsList = new ArrayList<>();
	private Vec3 intersection;

	public EyeOfEnderHelper() {
		super("Eye of Ender Helper", Category.DETECTION, "Records eye throws and computes the stronghold line intersection.");
		addSettings(drawLines, showPrediction, autoRecord);
	}

	@Override
	public void onTick() {
		if (!autoRecord.isOn() || mc.level == null) {
			return;
		}
		// Watch newly spawned eye of ender entities
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof EyeOfEnder eye && !eye.isRemoved() && eye.tickCount < 20) {
				recordThrow(eye);
			}
		}
		if (throwsList.size() > 24) {
			throwsList.remove(0);
		}
	}

	private void recordThrow(EyeOfEnder eye) {
		Vec3 pos = eye.position();
		Vec3 delta = new Vec3(eye.getX() - eye.xo, eye.getY() - eye.yo, eye.getZ() - eye.zo);
		if (delta.horizontalDistanceSqr() < 1.0e-4) {
			return;
		}
		Vec3 flat = new Vec3(delta.x, 0, delta.z).normalize();
		for (Throw existing : throwsList) {
			if (existing.start.distanceToSqr(pos) < 4.0) {
				return; // already recorded this throw
			}
		}
		throwsList.add(new Throw(pos, flat));
		recompute();
	}

	/** Least-squares intersection of 2D lines (p + t*d). */
	private void recompute() {
		if (throwsList.size() < 2) {
			intersection = null;
			return;
		}
		// Solve for point p minimizing sum over lines of perpendicular distance^2:
		// Sum_i ( (I - d_i d_i^T) p ) = Sum_i ( (I - d_i d_i^T) q_i ), q_i = point on line
		double a11 = 0, a12 = 0, a22 = 0, b1 = 0, b2 = 0;
		for (Throw t : throwsList) {
			double dx = t.direction.x;
			double dz = t.direction.z;
			double qx = t.start.x;
			double qz = t.start.z;
			double m11 = 1 - dx * dx;
			double m12 = -dx * dz;
			double m22 = 1 - dz * dz;
			a11 += m11;
			a12 += m12;
			a22 += m22;
			b1 += m11 * qx + m12 * qz;
			b2 += m12 * qx + m22 * qz;
		}
		double det = a11 * a22 - a12 * a12;
		if (Math.abs(det) < 1.0e-9) {
			intersection = null;
			return;
		}
		double px = (a22 * b1 - a12 * b2) / det;
		double pz = (a11 * b2 - a12 * b1) / det;
		intersection = new Vec3(px, mc.player == null ? 64 : mc.player.getY(), pz);
		}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			if (drawLines.isOn()) {
				for (Throw t : throwsList) {
					Vec3 end = t.start.add(t.direction.scale(200));
					WorldShapes.line(t.start.x, t.start.y, t.start.z, end.x, t.start.y, end.z,
							Colors.withAlpha(Colors.ACCENT, 0.7f), true);
					WorldShapes.line(t.start.x, t.start.y, t.start.z, t.start.x, 0, t.start.z,
							Colors.withAlpha(Colors.ACCENT, 0.25f), true);
				}
			}
			if (showPrediction.isOn() && intersection != null) {
				WorldShapes.filledBox(intersection.x - 1, intersection.y, intersection.z - 1,
						intersection.x + 1, intersection.y + 3, intersection.z + 1,
						Colors.withAlpha(Colors.SUCCESS, 0.3f), true);
				WorldShapes.outlinedBox(intersection.x - 1, intersection.y, intersection.z - 1,
						intersection.x + 1, intersection.y + 3, intersection.z + 1, Colors.SUCCESS, true);
				WorldShapes.text("Stronghold line intersection", intersection.x, intersection.y + 3.5, intersection.z,
						1.1f, Colors.WHITE, 0x90000000, true);
			}
		} finally {
			WorldShapes.end();
		}
	}

	public void clear() {
		throwsList.clear();
		intersection = null;
	}

}
