package dev.rydonut.gui.widgets;

import dev.rydonut.gui.Theme;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.EnumSetting;
import dev.rydonut.settings.IntSetting;
import dev.rydonut.settings.KeybindSetting;
import dev.rydonut.settings.Setting;
import dev.rydonut.settings.StringListSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for the clickable rows that render one {@link Setting}.
 *
 * <p>Widgets are position-less: the owning panel supplies x/y/width during
 * {@link #render}; the widget remembers its last geometry so hit tests work
 * between frames. {@link #render} returns the consumed height, which always
 * equals {@link #getHeight()}.
 */
public abstract class SettingWidget {
        protected final Setting<?> setting;

        protected int lastX;
        protected int lastY;
        protected int lastWidth;
        protected int lastHeight;
        protected boolean hovered;

        protected SettingWidget(Setting<?> setting) {
                this.setting = setting;
        }

        public Setting<?> setting() {
                return setting;
        }

        public abstract int getHeight();

        public abstract int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta);

        /** Returns true when the click was consumed. */
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
        }

        /** Returns true when the drag was consumed. */
        public boolean mouseDragged(double mouseX, double mouseY) {
                return false;
        }

        /** Returns true when the key was consumed (used for keybind capture). */
        public boolean keyPressed(int keyCode) {
                return false;
        }

        /** Returns true when the widget is currently capturing input (keybinds). */
        public boolean isCapturing() {
                return false;
        }

        /** Called when a click lands outside the widget while it is capturing. */
        public void cancelCapture() {
        }

        /** Returns true when the scroll was consumed (list widgets). */
        public boolean mouseScrolled(double mouseX, double mouseY, float scrollY) {
                return false;
        }

        public boolean inBounds(double mouseX, double mouseY) {
                return mouseX >= lastX && mouseX < lastX + lastWidth
                                && mouseY >= lastY && mouseY < lastY + lastHeight;
        }

        /** Records the current geometry and hover state; call at the top of render. */
        protected void track(int x, int y, int width, int height, double mouseX, double mouseY) {
                lastX = x;
                lastY = y;
                lastWidth = width;
                lastHeight = height;
                hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }

        /** Shared row background + setting name used by most widgets. */
        protected void drawRow(GuiGraphics graphics, Font font, int x, int y, int width, String name) {
                if (hovered) {
                        graphics.fill(x + 2, y, x + width - 2, y + getHeight(), Theme.hoveredRow());
                }
                graphics.drawString(font, name, x + Theme.PADDING, y + 5, hovered ? Theme.white() : Theme.textDim());
        }

        /** Builds the right widget for a setting; unknown types get a read-only row. */
        public static SettingWidget create(Setting<?> setting) {
                if (setting instanceof BoolSetting bool) {
                        return new ToggleWidget(bool);
                }
                if (setting instanceof IntSetting intSetting) {
                        return new SliderWidget(intSetting);
                }
                if (setting instanceof DoubleSetting doubleSetting) {
                        return new SliderWidget(doubleSetting);
                }
                if (setting instanceof EnumSetting<?> enumSetting) {
                        return new EnumWidget(enumSetting);
                }
                if (setting instanceof ColorSetting colorSetting) {
                        return new ColorWidget(colorSetting);
                }
                if (setting instanceof KeybindSetting keybindSetting) {
                        return new KeybindWidget(keybindSetting);
                }
                if (setting instanceof StringListSetting listSetting) {
                        return new ListWidget(listSetting);
                }
                return new ValueWidget(setting);
        }

        /** Fallback row: shows the setting name and its current value, read-only. */
        private static final class ValueWidget extends SettingWidget {
                ValueWidget(Setting<?> setting) {
                        super(setting);
                }

                @Override
                public int getHeight() {
                        return Theme.ITEM_HEIGHT;
                }

                @Override
                public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                        track(x, y, width, getHeight(), mouseX, mouseY);
                        drawRow(graphics, font, x, y, width, setting.getName());
                        String value = String.valueOf(setting.get());
                        int available = width - Theme.PADDING * 2 - font.width(setting.getName());
                        if (available > 8 && !value.isEmpty()) {
                                value = Theme.ellipsize(font, value, available);
                                graphics.drawString(font, value, x + width - Theme.PADDING - font.width(value), y + 5, Theme.textDim());
                        }
                        return getHeight();
                }
        }
}
