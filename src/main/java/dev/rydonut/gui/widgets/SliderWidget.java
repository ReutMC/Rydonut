package dev.rydonut.gui.widgets;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.gui.Theme;
import dev.rydonut.render.Colors;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.IntSetting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Drag slider for {@link IntSetting} and {@link DoubleSetting}: name and value
 * on the top line, a PRIMARY-filled track below. The whole row is grabbable.
 */
public class SliderWidget extends SettingWidget {
        private static final int HEIGHT = 24;

        private final IntSetting intSetting;
        private final DoubleSetting doubleSetting;
        private boolean dragging;

        public SliderWidget(IntSetting setting) {
                super(setting);
                this.intSetting = setting;
                this.doubleSetting = null;
        }

        public SliderWidget(DoubleSetting setting) {
                super(setting);
                this.intSetting = null;
                this.doubleSetting = setting;
        }

        @Override
        public int getHeight() {
                return HEIGHT;
        }

        private float value01() {
                if (intSetting != null) {
                        int range = Math.max(1, intSetting.getMax() - intSetting.getMin());
                        return (float) (intSetting.get() - intSetting.getMin()) / range;
                }
                double range = Math.max(1.0E-6D, doubleSetting.getMax() - doubleSetting.getMin());
                return (float) ((doubleSetting.get() - doubleSetting.getMin()) / range);
        }

        private void setValue01(float t) {
                t = Mth.clamp(t, 0.0F, 1.0F);
                if (intSetting != null) {
                        int range = intSetting.getMax() - intSetting.getMin();
                        int step = Math.max(1, intSetting.getStep());
                        int raw = intSetting.getMin() + Math.round(t * range);
                        int snapped = intSetting.getMin() + Math.round((raw - intSetting.getMin()) / (float) step) * step;
                        intSetting.set(Mth.clamp(snapped, intSetting.getMin(), intSetting.getMax()));
                } else {
                        double range = doubleSetting.getMax() - doubleSetting.getMin();
                        double step = doubleSetting.getStep() > 0.0D ? doubleSetting.getStep() : 0.0D;
                        double value = doubleSetting.getMin() + t * range;
                        if (step > 0.0D) {
                                value = doubleSetting.getMin() + Math.round((value - doubleSetting.getMin()) / step) * step;
                        }
                        doubleSetting.set(Mth.clamp(value, doubleSetting.getMin(), doubleSetting.getMax()));
                }
                ConfigManager.markDirty();
        }

        private String display() {
                return intSetting != null ? String.valueOf(intSetting.get()) : doubleSetting.format();
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, HEIGHT, mouseX, mouseY);
                if (hovered) {
                        graphics.fill(x + 2, y, x + width - 2, y + HEIGHT, Theme.hoveredRow());
                }
                graphics.drawString(font, setting.getName(), x + Theme.PADDING, y + 3, hovered ? Theme.white() : Theme.textDim());
                String value = display();
                graphics.drawString(font, value, x + width - Theme.PADDING - font.width(value), y + 3, Theme.accent());

                int tx1 = x + Theme.PADDING;
                int tx2 = x + width - Theme.PADDING;
                int ty = y + 15;
                graphics.fill(tx1, ty, tx2, ty + 5, Theme.TRACK);
                int fillX = tx1 + Math.round((tx2 - tx1) * value01());
                int fillColor = Colors.lerp(Theme.primary(), Theme.accent(), hovered || dragging ? 0.35F : 0.0F);
                if (fillX > tx1) {
                        graphics.fill(tx1, ty, fillX, ty + 5, fillColor);
                }
                graphics.fill(Mth.clamp(fillX - 1, tx1, tx2 - 2), ty - 1, Mth.clamp(fillX + 1, tx1 + 2, tx2), ty + 6, Theme.white());
                return HEIGHT;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0 && inBounds(mouseX, mouseY)) {
                        dragging = true;
                        setValue01(ratio(mouseX));
                        return true;
                }
                return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY) {
                if (dragging) {
                        setValue01(ratio(mouseX));
                        return true;
                }
                return false;
        }

        @Override
        public void mouseReleased(double mouseX, double mouseY, int button) {
                dragging = false;
        }

        private float ratio(double mouseX) {
                int tx1 = lastX + Theme.PADDING;
                int tx2 = lastX + lastWidth - Theme.PADDING;
                if (tx2 <= tx1) {
                        return 0.0F;
                }
                return (float) ((mouseX - tx1) / (double) (tx2 - tx1));
        }
}
