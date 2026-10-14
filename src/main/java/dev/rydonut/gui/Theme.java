package dev.rydonut.gui;

import dev.rydonut.render.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * ClickGUI theme: shared dimensions, palette accessors over
 * {@link dev.rydonut.render.Colors} and small draw helpers.
 */
public final class Theme {
        public static final int PANEL_WIDTH = 210;
        public static final int ITEM_HEIGHT = 18;
        public static final int TITLE_HEIGHT = 22;
        public static final int PADDING = 6;

        /** Exponential approach factor applied per partial tick. */
        public static final float ANIM_SPEED = 0.45f;

        /** Dark track drawn behind sliders / toggle pills. */
        public static final int TRACK = 0xFF0A1420;

        private Theme() {
        }

        public static int panelBg() {
                return Colors.PANEL;
        }

        public static int panelLight() {
                return Colors.PANEL_LIGHT;
        }

        public static int outline() {
                return Colors.OUTLINE;
        }

        public static int primary() {
                return Colors.PRIMARY;
        }

        public static int accent() {
                return Colors.ACCENT;
        }

        public static int textDim() {
                return Colors.TEXT_DIM;
        }

        public static int white() {
                return Colors.WHITE;
        }

        public static int danger() {
                return Colors.DANGER;
        }

        public static int success() {
                return Colors.SUCCESS;
        }

        /** Row background for the currently hovered row. */
        public static int hoveredRow() {
                return Colors.lerp(Colors.PANEL, Colors.PANEL_LIGHT, 0.65f);
        }

        /** Exponential smoothing towards a target; delta is the render partial tick. */
        public static float approach(float current, float target, float delta) {
                float t = Math.min(1.0f, Math.abs(delta) * ANIM_SPEED);
                return current + (target - current) * t;
        }

        /**
         * Cheap "rounded" panel: a 1px outline drawn as a cross, with the body
         * inset by one pixel, so the four corner pixels stay empty and the panel
         * reads as slightly rounded. Uses only {@link GuiGraphics#fill}.
         */
        public static void drawRoundedPanel(GuiGraphics graphics, int x, int y, int w, int h) {
                drawRoundedPanel(graphics, x, y, w, h, Colors.PANEL, Colors.OUTLINE);
        }

        public static void drawRoundedPanel(GuiGraphics graphics, int x, int y, int w, int h, int bg, int border) {
                if (w < 4 || h < 4) {
                        graphics.fill(x, y, x + w, y + h, bg);
                        return;
                }
                // outline pass (corner pixels intentionally skipped -> rounded illusion)
                graphics.fill(x + 1, y, x + w - 1, y + h, border);
                graphics.fill(x, y + 1, x + w, y + h - 1, border);
                // body pass, inset by 1px on every edge
                graphics.fill(x + 2, y + 1, x + w - 2, y + h - 1, bg);
                graphics.fill(x + 1, y + 2, x + w - 1, y + h - 2, bg);
        }

        /** Clips a string to the given pixel width, appending "..." when cut. */
        public static String ellipsize(Font font, String text, int maxWidth) {
                if (text == null || text.isEmpty() || font.width(text) <= maxWidth) {
                        return text;
                }
                String cut = text;
                while (cut.length() > 1 && font.width(cut + "...") > maxWidth) {
                        cut = cut.substring(0, cut.length() - 1);
                }
                return cut + "...";
        }
}
