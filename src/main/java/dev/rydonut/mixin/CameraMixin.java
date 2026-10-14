package dev.rydonut.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;

import dev.rydonut.modules.visuals.Freecam;

/**
 * Freecam support: after the vanilla camera is posed, the freecam module may
 * override rotation and position. Visual-only: the real player is untouched
 * and no packets are involved.
 *
 * <p>The camera setters are protected, so this mixin (which merges into
 * Camera) applies the freecam pose through {@code @Shadow} methods.
 *
 * <p>Target method verified in the official 1.21.11 Mojang mappings:
 * {@code void Camera.setup(Level, Entity, boolean, boolean, float)}.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "setup", at = @At("RETURN"))
	private void rydonut$freecamOverride(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity focus,
			boolean detached, boolean thirdPersonReverse, float tickDelta, CallbackInfo ci) {
		float[] pose = Freecam.desiredPose();
		if (pose != null) {
			this.setRotation(pose[0], pose[1]);
			this.setPosition(pose[2], pose[3], pose[4]);
		}
	}
}
