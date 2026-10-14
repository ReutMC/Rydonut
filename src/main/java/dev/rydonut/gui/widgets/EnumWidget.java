package dev.rydonut.gui.widgets;

import java.util.Locale;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.gui.Theme;
import dev.rydonut.settings.EnumSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Enum setting row: name on the left, prettified current value on the right.
 * Left-click cycles forwards, right-click cycles backwards.
 */
public class EnumWidget extends SettingWidget {
        private final EnumSetting<?> enumSetting;
        private Object lastValue;
        private String lastDisplay = "";

        public EnumWidget(EnumSetting<?> setting) {
                super(setting);
                this.enumSetting = setting;
        }

        @Override
        public int getHeight() {
                return Theme.ITEM_HEIGHT;
        }

        private String display() {
                Object current = enumSetting.get();
                if (current != lastValue) {
                        lastValue = current;
                        lastDisplay = pretty(String.valueOf(current));
                }
                return lastDisplay;
        }

        /** "ESP_BOX" -> "Esp Box". */
        static String pretty(String enumName) {
                String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
                StringBuilder sb = new StringBuilder(s.length());
                boolean capitalize = true;
                for (int i = 0; i < s.length(); i++) {
                        char c = s.charAt(i);
                        sb.append(capitalize ? Character.toUpperCase(c) : c);
                        capitalize = c == ' ';
                }
                return sb.toString();
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, getHeight(), mouseX, mouseY);
                drawRow(graphics, font, x, y, width, setting.getName());
                String value = Theme.ellipsize(font, display(), width / 2);
                graphics.drawString(font, value, x + width - Theme.PADDING - font.width(value), y + 5, Theme.accent());
                return getHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (!inBounds(mouseX, mouseY)) {
                        return false;
                }
                if (button == 0) {
                        enumSetting.cycle();
                } else if (button == 1) {
                        cycleBackward(enumSetting);
                } else {
                        return false;
                }
                ConfigManager.markDirty();
                return true;
        }

        /** Generic capture helper so a wildcard enum setting can be set. */
        private static <E extends Enum<E>> void cycleBackward(EnumSetting<E> setting) {
                E[] values = setting.values();
                E previous = values[(setting.get().ordinal() - 1 + values.length) % values.length];
                setting.set(previous);
        }
}
