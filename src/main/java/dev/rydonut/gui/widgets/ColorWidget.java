package dev.rydonut.gui.widgets;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.gui.Theme;
import dev.rydonut.render.Colors;
import dev.rydonut.settings.ColorSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Color setting row: name + swatch when collapsed; clicking expands four
 * small channel sliders (R, G, B and A when alpha is allowed). Channels are
 * extracted from / reassembled into the ARGB int of the setting.
 */
public class ColorWidget extends SettingWidget {
        private static final int CHANNEL_HEIGHT = 13;
        private static final String[] CHANNEL_NAMES = {"R", "G", "B", "A"};
        private static final int[] CHANNEL_COLORS = {0xFFFF4040, 0xFF40FF60, 0xFF4090FF, 0xFFB9C8D6};

        private final ColorSetting color;
        private boolean expanded;
        private int draggingChannel = -1;

        public ColorWidget(ColorSetting setting) {
                super(setting);
                this.color = setting;
        }

        private int channelCount() {
                return color.allowsAlpha() ? 4 : 3;
        }

        @Override
        public int getHeight() {
                return expanded ? Theme.ITEM_HEIGHT + channelCount() * CHANNEL_HEIGHT + 6 : Theme.ITEM_HEIGHT;
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, getHeight(), mouseX, mouseY);
                if (hovered) {
                        graphics.fill(x + 2, y, x + width - 2, y + Theme.ITEM_HEIGHT, Theme.hoveredRow());
                }
                graphics.drawString(font, color.getName(), x + Theme.PADDING, y + 5, hovered ? Theme.white() : Theme.textDim());
                graphics.drawString(font, expanded ? "v" : ">", x + width - 12, y + 5, Theme.textDim());

                int swatchW = 14;
                int swatchH = 10;
                int sx = x + width - 24 - swatchW;
                int sy = y + 4;
                graphics.fill(sx - 1, sy - 1, sx + swatchW + 1, sy + swatchH + 1, Theme.outline());
                graphics.fill(sx, sy, sx + swatchW, sy + swatchH, color.get());

                int cy = y + Theme.ITEM_HEIGHT;
                if (expanded) {
                        int argb = color.get();
                        int rr = Colors.red(argb);
                        int gg = Colors.green(argb);
                        int bb = Colors.blue(argb);
                        int aa = Colors.alpha(argb);
                        int tx1 = x + 22;
                        int tx2 = x + width - 26;
                        for (int channel = 0; channel < channelCount(); channel++) {
                                int value = switch (channel) {
                                        case 0 -> rr;
                                        case 1 -> gg;
                                        case 2 -> bb;
                                        default -> aa;
                                };
                                graphics.drawString(font, CHANNEL_NAMES[channel], x + 10, cy + 3, Theme.textDim());
                                graphics.fill(tx1, cy + 4, tx2, cy + 9, Theme.TRACK);
                                int fillX = tx1 + Math.round((tx2 - tx1) * (value / 255.0F));
                                if (fillX > tx1) {
                                        graphics.fill(tx1, cy + 4, fillX, cy + 9, CHANNEL_COLORS[channel]);
                                }
                                graphics.fill(Mth.clamp(fillX - 1, tx1, tx2 - 2), cy + 3, Mth.clamp(fillX + 1, tx1 + 2, tx2), cy + 10, Theme.white());
                                String num = String.valueOf(value);
                                graphics.drawString(font, num, x + width - 22, cy + 3, Theme.textDim());
                                cy += CHANNEL_HEIGHT;
                        }
                }
                return getHeight();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0 || !inBounds(mouseX, mouseY)) {
                        return false;
                }
                if (mouseY < lastY + Theme.ITEM_HEIGHT) {
                        expanded = !expanded;
                        return true;
                }
                int channel = channelAt(mouseX, mouseY);
                if (channel >= 0) {
                        draggingChannel = channel;
                        setChannel(channel, mouseX);
                        return true;
                }
                return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
                if (draggingChannel >= 0) {
                        setChannel(draggingChannel, mouseX);
                        return true;
                }
                return false;
        }

        @Override
        public void mouseReleased(double mouseX, double mouseY, int button) {
                draggingChannel = -1;
        }

        private int channelAt(double mouseX, double mouseY) {
                if (!expanded) {
                        return -1;
                }
                int index = (int) Math.floor((mouseY - (lastY + Theme.ITEM_HEIGHT)) / CHANNEL_HEIGHT);
                if (index < 0 || index >= channelCount()) {
                        return -1;
                }
                int tx1 = lastX + 22;
                int tx2 = lastX + lastWidth - 26;
                if (mouseX < tx1 - 4 || mouseX > tx2 + 4) {
                        return -1;
                }
                return index;
        }

        private void setChannel(int channel, double mouseX) {
                int tx1 = lastX + 22;
                int tx2 = lastX + lastWidth - 26;
                if (tx2 <= tx1) {
                        return;
                }
                float t = (float) Mth.clamp((mouseX - tx1) / (double) (tx2 - tx1), 0.0D, 1.0D);
                int v = Math.round(t * 255.0F);
                int argb = color.get();
                int rr = Colors.red(argb);
                int gg = Colors.green(argb);
                int bb = Colors.blue(argb);
                int aa = Colors.alpha(argb);
                switch (channel) {
                        case 0 -> rr = v;
                        case 1 -> gg = v;
                        case 2 -> bb = v;
                        default -> aa = v;
                }
                color.set((aa << 24) | (rr << 16) | (gg << 8) | bb);
                ConfigManager.markDirty();
        }
}
