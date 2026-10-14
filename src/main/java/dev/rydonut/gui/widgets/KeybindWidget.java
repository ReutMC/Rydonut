package dev.rydonut.gui.widgets;

import dev.rydonut.gui.Theme;
import dev.rydonut.settings.KeybindSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind row: shows {@link KeybindSetting#describe()}; clicking starts
 * capture mode ("Press a key..."). While capturing, the screen routes keys
 * here: ESC cancels, Backspace clears to unbound, anything else binds.
 */
public class KeybindWidget extends SettingWidget {
        private final KeybindSetting keybind;

        public KeybindWidget(KeybindSetting setting) {
                super(setting);
                this.keybind = setting;
        }

        @Override
        public int getHeight() {
                return Theme.ITEM_HEIGHT;
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, getHeight(), mouseX, mouseY);
                drawRow(graphics, font, x, y, width, keybind.getName());
                boolean blink = (System.currentTimeMillis() / 400L) % 2L == 0L;
                String label = keybind.isListening() ? (blink ? "Press a key..." : "") : keybind.describe();
                if (!label.isEmpty()) {
                        int color = keybind.isListening() ? Theme.accent() : keybind.isBound() ? Theme.white() : Theme.textDim();
                        graphics.drawString(font, label, x + width - Theme.PADDING - font.width(label), y + 5, color);
                }
                return getHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && inBounds(mouseX, mouseY)) {
                        keybind.setListening(true);
                        return true;
                }
                return false;
        }

        @Override
        public boolean isCapturing() {
                return keybind.isListening();
        }

        @Override
        public void cancelCapture() {
                keybind.setListening(false);
        }

        @Override
        public boolean keyPressed(int keyCode) {
                if (!keybind.isListening()) {
                        return false;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                        keybind.setListening(false);
                } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                        keybind.set(0);
                        keybind.setListening(false);
                } else if (keyCode > 0) {
                        keybind.set(keyCode);
                        keybind.setListening(false);
                }
                return true;
        }
}
