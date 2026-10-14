package dev.rydonut.modules.visuals;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam (visual only): detaches the camera from the player and lets you fly
 * it around with the movement keys + mouse while the real player stays put.
 *
 * <p>Implementation is purely client-side: a small mixin overrides the camera
 * pose after vanilla positions it; the client player is frozen in place while
 * active so it does not wander. No packets are sent or modified.
 */
public class Freecam extends Module {
	private final DoubleSetting speed = new DoubleSetting("Speed", 1.0, 0.2, 5.0, 0.2);
	private final BoolSetting showPlayer = new BoolSetting("Render self hint", true);

	private Vec3 freecamPos;
	private float freecamYaw;
	private float freecamPitch;
	private float lastRawYaw;
	private float lastRawPitch;
	private double storedX, storedY, storedZ;
	private float storedYaw, storedPitch;

	public Freecam() {
		super("Freecam", Category.MISC, "Visual-only camera detachment (WASD + mouse).");
		addSettings(speed, showPlayer);
	}

	@Override
	protected void onEnable() {
		if (mc.player == null) {
			setEnabled(false);
			return;
		}
		freecamPos = mc.player.getEyePosition();
		freecamYaw = mc.player.getYRot();
		freecamPitch = mc.player.getXRot();
		lastRawYaw = freecamYaw;
		lastRawPitch = freecamPitch;
		storedX = mc.player.getX();
		storedY = mc.player.getY();
		storedZ = mc.player.getZ();
		storedYaw = mc.player.getYRot();
		storedPitch = mc.player.getXRot();
		ConfigManager.RydonutLogger.info("Freecam enabled");
	}

	@Override
	protected void onDisable() {
		freecamPos = null;
	}

	@Override
	public void onTick() {
		if (mc.player == null || freecamPos == null) {
			return;
		}
		// Accumulate mouse look deltas (vanilla applies mouse turning to the
		// camera entity in real time; we convert those deltas to freecam rotation)
		float rawYaw = mc.player.getYRot();
		float rawPitch = mc.player.getXRot();
		freecamYaw += rawYaw - lastRawYaw;
		freecamPitch = Mth.clamp(freecamPitch + (rawPitch - lastRawPitch), -90.0f, 90.0f);
		lastRawYaw = rawYaw;
		lastRawPitch = rawPitch;

		// Freeze the real player (client-side only)
		mc.player.setPos(storedX, storedY, storedZ);
		mc.player.setYRot(storedYaw);
		mc.player.setXRot(storedPitch);
		mc.player.setDeltaMovement(0, 0, 0);
		mc.player.fallDistance = 0;

		// WASD/Space/Shift movement relative to freecam yaw
		net.minecraft.client.Options options = mc.options;
		float forward = 0;
		float strafe = 0;
		if (options.keyUp.isDown()) {
			forward += 1;
		}
		if (options.keyDown.isDown()) {
			forward -= 1;
		}
		if (options.keyLeft.isDown()) {
			strafe += 1;
		}
		if (options.keyRight.isDown()) {
			strafe -= 1;
		}
		double vertical = 0;
		if (options.keyJump.isDown()) {
			vertical += 1;
		}
		if (options.keyShift.isDown()) {
			vertical -= 1;
		}
		double boost = options.keySprint.isDown() ? 2.5 : 1.0;
		double dist = speed.get() * boost * 0.6;
		if (forward != 0 || strafe != 0) {
			double yawRad = Math.toRadians(freecamYaw);
			double dx = (-Mth.sin((float) yawRad) * forward + Mth.cos((float) yawRad) * strafe) * dist;
			double dz = (Mth.cos((float) yawRad) * forward + Mth.sin((float) yawRad) * strafe) * dist;
			freecamPos = freecamPos.add(dx, 0, dz);
		}
		if (vertical != 0) {
			freecamPos = freecamPos.add(0, vertical * dist, 0);
		}
	}

	/**
	 * Called by the camera mixin after vanilla camera setup.
	 * Returns {yaw, pitch, x, y, z} or null when freecam is not active.
	 * The setters on Camera are protected, so the mixin (which merges into
	 * Camera) applies them via @Shadow.
	 */
	public static float[] desiredPose() {
		Freecam freecam = (Freecam) dev.rydonut.core.ModuleManager.get().getByName("Freecam");
		if (freecam == null || !freecam.isEnabled() || freecam.freecamPos == null) {
			return null;
		}
		return new float[]{
				freecam.freecamYaw, freecam.freecamPitch,
				(float) freecam.freecamPos.x, (float) freecam.freecamPos.y, (float) freecam.freecamPos.z};
	}

	public Vec3 position() {
		return freecamPos;
	}

	public boolean renderSelfHint() {
		return showPlayer.isOn();
	}
}
