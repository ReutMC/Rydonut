package dev.rydonut.modules.misc;

import org.lwjgl.glfw.GLFW;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.gui.ClickGUIScreen;
import dev.rydonut.settings.KeybindSetting;
import net.minecraft.client.Minecraft;

/**
 * Opens the ClickGUI. Default keybind is Right Shift (rebindable in the GUI).
 */
public class ClickGuiModule extends Module {
	public ClickGuiModule() {
		super("ClickGUI", Category.MISC, "Opens the module GUI. Default: Right Shift.");
		getKeybind().set(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	@Override
	public boolean togglesOnPress() {
		return true;
	}

	@Override
	protected void onEnable() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new ClickGUIScreen());
		// the screen replaces itself; immediately disable again so the
		// ArrayList does not show a permanent "ClickGUI" entry
		setEnabled(false);
	}
}
