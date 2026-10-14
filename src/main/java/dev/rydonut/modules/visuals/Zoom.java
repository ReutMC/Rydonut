package dev.rydonut.modules.visuals;

import org.lwjgl.glfw.GLFW;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import net.minecraft.util.Mth;

/**
 * Zoom: hold the module keybind to smoothly zoom the camera FOV.
 * Works through the GameRendererMixin (verified getFov hook).
 */
public class Zoom extends Module {
	private final DoubleSetting factor = new DoubleSetting("Zoom factor", 4.0, 1.5, 10.0, 0.5);
	private final BoolSetting smooth = new BoolSetting("Smooth", true);

	private float currentMultiplier = 1.0f;

	public Zoom() {
		super("Zoom", Category.MISC, "Hold the keybind to zoom (default rebindable).");
		addSettings(factor, smooth);
	}

	public static boolean isZooming() {
		Zoom zoom = (Zoom) dev.rydonut.core.ModuleManager.get().getByName("Zoom");
		if (zoom == null || !zoom.isEnabled() || !zoom.getKeybind().isBound()) {
			return false;
		}
		return org.lwjgl.glfw.GLFW.glfwGetKey(
				net.minecraft.client.Minecraft.getInstance().getWindow().handle(),
				zoom.getKeybind().get()) == GLFW.GLFW_PRESS;
	}

	public static float getFovMultiplier() {
		Zoom zoom = (Zoom) dev.rydonut.core.ModuleManager.get().getByName("Zoom");
		if (zoom == null) {
			return 1.0f;
		}
		return zoom.currentMultiplier;
	}

	@Override
	public void onTick() {
		float target = 1.0f;
		if (isZooming()) {
			target = 1.0f / factor.get().floatValue();
		}
		if (smooth.isOn()) {
			currentMultiplier = Mth.lerp(0.35f, currentMultiplier, target);
		} else {
			currentMultiplier = target;
		}
	}

	@Override
	protected void onDisable() {
		currentMultiplier = 1.0f;
	}
}
