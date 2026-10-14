package dev.rydonut.gui.widgets;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.gui.Theme;
import dev.rydonut.render.Colors;
import dev.rydonut.settings.BoolSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Toggle row for {@link BoolSetting}: name on the left, an ON/OFF pill on the
 * right whose background smoothly lerps towards PRIMARY while turning on.
 */
public class ToggleWidget extends SettingWidget {
        private static final int PILL_WIDTH = 34;
        private static final int PILL_HEIGHT = 11;

        private final BoolSetting bool;
        private float anim;

        public ToggleWidget(BoolSetting setting) {
                super(setting);
                this.bool = setting;
                this.anim = setting.isOn() ? 1.0f : 0.0f;
        }

        @Override
        public int getHeight() {
                return Theme.ITEM_HEIGHT;
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, getHeight(), mouseX, mouseY);
                anim = Theme.approach(anim, bool.isOn() ? 1.0f : 0.0f, delta);
                drawRow(graphics, font, x, y, width, bool.getName());

                int px = x + width - PILL_WIDTH - Theme.PADDING;
                int py = y + (Theme.ITEM_HEIGHT - PILL_HEIGHT) / 2;
                int bg = Colors.lerp(Theme.TRACK, Theme.primary(), anim);
                // rounded pill
                graphics.fill(px + 1, py, px + PILL_WIDTH - 1, py + PILL_HEIGHT, bg);
                graphics.fill(px, py + 1, px + PILL_WIDTH, py + PILL_HEIGHT - 1, bg);

                String label = bool.isOn() ? "ON" : "OFF";
                int labelColor = Colors.lerp(Theme.textDim(), Theme.white(), anim);
                graphics.drawString(font, label, px + (PILL_WIDTH - font.width(label)) / 2, py + 2, labelColor);
                return getHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && inBounds(mouseX, mouseY)) {
                        bool.set(!bool.isOn());
                        ConfigManager.markDirty();
                        return true;
                }
                return false;
        }
}
