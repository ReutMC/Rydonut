package dev.rydonut.modules.visuals;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.settings.DoubleSetting;

/**
 * Fullbright: pushes the game's gamma (brightness) option to its maximum.
 *
 * <p>Note on 1.21.11: the lightmap pipeline was reworked internally
 * (the old LightmapTextureManager injection point no longer exists in the
 * official mappings), so this module uses the verified public
 * {@code Options#gamma()} OptionInstance instead of shader-level fullbright.
 * It sets gamma to 1.0 (the "Moody/Bright" slider max) and restores the
 * previous value on disable.
 */
public class Fullbright extends Module {
	private final DoubleSetting min = new DoubleSetting("Preset gamma", 1.0, 0.0, 1.0, 0.1);

	private double previousGamma = -1;

	public Fullbright() {
		super("Fullbright", Category.MISC, "Maximum brightness (gamma 1.0). Restores on disable.");
		addSettings(min);
	}

	@Override
	protected void onEnable() {
		try {
			previousGamma = mc.options.gamma().get();
			mc.options.gamma().set(min.get());
			ConfigManager.RydonutLogger.info("Fullbright on (gamma " + min.get() + ")");
		} catch (Exception e) {
			ConfigManager.RydonutLogger.warn("Fullbright failed: " + e);
		}
	}

	@Override
	protected void onDisable() {
		try {
			if (previousGamma >= 0) {
				mc.options.gamma().set(previousGamma);
				previousGamma = -1;
			}
		} catch (Exception ignored) {
		}
	}
}
