package dev.rydonut.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side projectile trajectory prediction (pure math, no packets).
 */
public final class TrajectorySim {
	public static final class Result {
		public final List<Vec3> points = new ArrayList<>();
		public Vec3 landing;
		public boolean hitsGround;
	}

	private TrajectorySim() {
	}

	/**
	 * Simulates a thrown projectile.
	 *
	 * @param start    eye position
	 * @param velocity initial velocity (blocks/tick)
	 * @param gravity  blocks/tick^2 (pearl/egg/snowball 0.03, arrow 0.05)
	 * @param drag     velocity multiplier per tick (0.99 typical)
	 */
	public static Result simulate(Vec3 start, Vec3 velocity, double gravity, double drag, net.minecraft.world.level.Level level, int maxTicks) {
		Result result = new Result();
		Vec3 pos = start;
		Vec3 vel = velocity;
		for (int i = 0; i < maxTicks; i++) {
			result.points.add(pos);
			Vec3 next = pos.add(vel);
			// simple block collision check at the next position
			BlockPosLike bp = new BlockPosLike(next);
			if (!level.getBlockState(bp.pos()).isAir() && level.getBlockState(bp.pos()).canOcclude()) {
				result.landing = pos;
				result.hitsGround = true;
				break;
			}
			// water drag approximation ignored; keep it simple and visual
			pos = next;
			vel = new Vec3(vel.x * drag, vel.y * drag - gravity, vel.z * drag);
		}
		if (result.landing == null) {
			result.landing = pos;
		}
		return result;
	}

	public static double pearlGravity() {
		return 0.03;
	}

	public static double arrowGravity() {
		return 0.05;
	}

	public static double genericDrag() {
		return 0.99;
	}

	/** Initial velocity for a thrown pearl/egg/snowball given charge ticks is fixed; arrows scale with charge. */
	public static Vec3 throwVelocity(Vec3 look, float charge01) {
		float power = Mth.clamp(charge01, 0.1f, 1.0f);
		double speed = 1.5 + power * 1.5;
		return look.scale(speed);
	}

	/** tiny helper record */
	private record BlockPosLike(net.minecraft.core.BlockPos pos) {
		BlockPosLike(Vec3 v) {
			this(net.minecraft.core.BlockPos.containing(v.x, v.y, v.z));
		}
	}
}
