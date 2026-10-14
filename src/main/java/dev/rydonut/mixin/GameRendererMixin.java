package dev.rydonut.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import dev.rydonut.modules.visuals.Zoom;

/**
 * Zoom module support. Multiplies the world FOV while zoom is active.
 *
 * <p>Target method verified in the official 1.21.11 Mojang mappings:
 * {@code float GameRenderer.getFov(Camera, float, boolean)}.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void rydonut$zoomFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
		if (Zoom.isZooming()) {
			cir.setReturnValue(cir.getReturnValueF() * Zoom.getFovMultiplier());
		}
	}
}
