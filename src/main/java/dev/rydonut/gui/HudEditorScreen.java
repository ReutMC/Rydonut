package dev.rydonut.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.hud.HudModule;
import dev.rydonut.render.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Drag-and-drop HUD editor: every HUD module is drawn as a preview box at its
 * current position. Left-click drag moves a module (snapped to a 2px grid),
 * right-click toggles it, ESC saves and returns to the ClickGUI.
 */
public class HudEditorScreen extends Screen {
        private static final int HINT_BAR_HEIGHT = 20;

        private final List<HudModule> hudModules = new ArrayList<>();
        private HudModule dragging;
        private double dragOffX;
        private double dragOffY;
        private int lastGuiWidth;
        private int lastGuiHeight;

        public HudEditorScreen() {
                super(Component.literal("Rydonut HUD Editor"));
                for (Module module : ModuleManager.get().getModulesIn(Category.HUD)) {
                        if (module instanceof HudModule hud) {
                                hudModules.add(hud);
                        }
                }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                renderBackground(graphics, mouseX, mouseY, delta);
                Font font = Minecraft.getInstance().font;
                lastGuiWidth = graphics.guiWidth();
                lastGuiHeight = graphics.guiHeight();

                // faint alignment grid (2px snap, lines every 32px)
                for (int gx = 0; gx < lastGuiWidth; gx += 32) {
                        graphics.fill(gx, HINT_BAR_HEIGHT, gx + 1, lastGuiHeight, 0x144FC3F7);
                }
                for (int gy = HINT_BAR_HEIGHT; gy < lastGuiHeight; gy += 32) {
                        graphics.fill(0, gy, lastGuiWidth, gy + 1, 0x144FC3F7);
                }

                // hint bar
                graphics.fill(0, 0, lastGuiWidth, HINT_BAR_HEIGHT - 1, 0xC80D1B2A);
                graphics.fill(0, HINT_BAR_HEIGHT - 1, lastGuiWidth, HINT_BAR_HEIGHT, Theme.outline());
                graphics.drawCenteredString(font, "HUD Editor - drag: left-click | toggle: right-click | close: ESC",
                                lastGuiWidth / 2, 6, Theme.white());

                for (HudModule module : hudModules) {
                        renderPreview(graphics, font, module, mouseX, mouseY);
                }

                if (dragging != null) {
                        String pos = dragging.getX() + ", " + dragging.getY();
                        int px = Mth.clamp(dragging.getX() + dragging.getWidth() + 4, 0, Math.max(0, lastGuiWidth - font.width(pos) - 2));
                        int py = Mth.clamp(dragging.getY(), HINT_BAR_HEIGHT, Math.max(HINT_BAR_HEIGHT, lastGuiHeight - 10));
                        graphics.drawString(font, pos, px, py, Theme.accent());
                }
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0xA00D1B2A);
        }

        private void renderPreview(GuiGraphics graphics, Font font, HudModule module, int mouseX, int mouseY) {
                int x = module.getX();
                int y = module.getY();
                int w = Math.max(module.getWidth(), 10);
                int h = Math.max(module.getHeight(), 8);
                boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
                boolean active = module == dragging;

                int bg = module.isEnabled() ? Colors.withAlpha(Theme.panelLight(), 0.85F) : Colors.withAlpha(Theme.panelBg(), 0.55F);
                int border;
                if (active) {
                        border = Theme.primary();
                } else if (hovered) {
                        border = Theme.accent();
                } else {
                        border = module.isEnabled() ? Colors.withAlpha(Theme.primary(), 0.55F) : Theme.outline();
                }

                graphics.fill(x, y, x + w, y + h, bg);
                graphics.fill(x, y, x + w, y + 1, border);
                graphics.fill(x, y + h - 1, x + w, y + h, border);
                graphics.fill(x, y + 1, x + 1, y + h - 1, border);
                graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, border);

                String name = module.getName() + (module.isEnabled() ? "" : " (off)");
                if (font.width(name) <= w - 4 && h >= 10) {
                        graphics.drawString(font, name, x + 2, y + (h - 8) / 2, module.isEnabled() ? Theme.white() : Theme.textDim());
                } else {
                        int ty = y >= HINT_BAR_HEIGHT + 12 ? y - 10 : y + h + 2;
                        int tx = Mth.clamp(x, 0, Math.max(0, lastGuiWidth - font.width(name)));
                        graphics.drawString(font, name, tx, ty, Theme.textDim());
                }
        }

        // ---------------------------------------------------------------- input

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
                double mx = event.x();
                double my = event.y();
                int button = event.button();
                for (int i = hudModules.size() - 1; i >= 0; i--) {
                        HudModule module = hudModules.get(i);
                        int x = module.getX();
                        int y = module.getY();
                        int w = Math.max(module.getWidth(), 10);
                        int h = Math.max(module.getHeight(), 8);
                        if (mx >= x && mx < x + w && my >= y && my < y + h) {
                                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                        dragging = module;
                                        dragOffX = mx - x;
                                        dragOffY = my - y;
                                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                                        module.toggle();
                                }
                                return true;
                        }
                }
                return super.mouseClicked(event, doubled);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
                if (dragging != null) {
                        int maxX = Math.max(0, lastGuiWidth - Math.max(dragging.getWidth(), 10));
                        int maxY = Math.max(HINT_BAR_HEIGHT, lastGuiHeight - Math.max(dragging.getHeight(), 8));
                        int nx = Mth.clamp((int) (event.x() - dragOffX), 0, maxX) & ~1; // snap to 2px grid
                        int ny = Mth.clamp((int) (event.y() - dragOffY), HINT_BAR_HEIGHT, maxY) & ~1;
                        if (nx < 0) {
                                nx = 0;
                        }
                        dragging.setPosition(nx, ny);
                        return true;
                }
                return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
                if (dragging != null) {
                        dragging = null;
                        return true;
                }
                return super.mouseReleased(event);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
                int key = event.key();
                if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                        onClose();
                        return true;
                }
                return super.keyPressed(event);
        }

        @Override
        public void onClose() {
                ConfigManager.saveProfile(ConfigManager.activeProfile());
                // return to the ClickGUI the editor was opened from
                Minecraft.getInstance().setScreen(new ClickGUIScreen());
        }
}
