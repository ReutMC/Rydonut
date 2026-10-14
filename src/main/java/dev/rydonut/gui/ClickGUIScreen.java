package dev.rydonut.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Rydonut ClickGUI: dark translucent backdrop, four draggable category
 * panels (Visuals, Detection, HUD, Misc), a Settings panel with profile
 * actions + HUD editor shortcut, and a search bar at the top that filters
 * modules across panels (matching modules auto-expand). Open/close is
 * animated; ESC or Right Shift closes and saves the active profile.
 */
public class ClickGUIScreen extends Screen {
        private static final int SEARCH_WIDTH = 240;
        private static final int SEARCH_HEIGHT = 16;
        private static final int SEARCH_Y = 8;
        private static final int PANEL_START_Y = 32;
        private static final int PANEL_GAP = 8;
        private static final int PANEL_MARGIN = 10;
        private static final int BACKDROP_RGB = 0x0D1B2A;

        /** 0..1 open amount, animated in render; static so reopens continue smoothly. */
        private static float openAnimation;

        private final List<Panel> panels = new ArrayList<>();
        private final SettingsPanel settingsPanel = new SettingsPanel();

        private String search = "";
        private boolean closing;
        private boolean layoutDirty = true;
        private int lastGuiWidth;
        private int lastGuiHeight;

        public ClickGUIScreen() {
                super(Component.literal("Rydonut ClickGUI"));
                panels.add(new Panel(Category.VISUALS));
                panels.add(new Panel(Category.DETECTION));
                panels.add(new Panel(Category.HUD));
                panels.add(new Panel(Category.MISC));
        }

        /** Opens the ClickGUI. */
        public static void open() {
                Minecraft.getInstance().setScreen(new ClickGUIScreen());
        }

        @Override
        public void init() {
                super.init();
                layoutDirty = true;
        }

        // ---------------------------------------------------------------- render

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                openAnimation = Theme.approach(openAnimation, closing ? 0.0F : 1.0F, delta);
                if (closing && openAnimation < 0.02F) {
                        openAnimation = 0.0F;
                        onClose();
                        return;
                }
                renderBackground(graphics, mouseX, mouseY, delta);

                Font font = Minecraft.getInstance().font;
                lastGuiWidth = graphics.guiWidth();
                lastGuiHeight = graphics.guiHeight();
                if (layoutDirty) {
                        layout();
                        layoutDirty = false;
                }

                // panels slide down into place while opening / up while closing
                float slide = -(1.0F - openAnimation) * 8.0F;
                var pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(0.0F, slide);
                for (Panel panel : panels) {
                        panel.render(graphics, font, mouseX, mouseY, delta);
                }
                settingsPanel.render(graphics, font, mouseX, mouseY, delta);
                pose.popMatrix();

                renderSearchBar(graphics, font);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                int alpha = (int) (0x90 * openAnimation);
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | BACKDROP_RGB);
        }

        private void renderSearchBar(GuiGraphics graphics, Font font) {
                int sx = (lastGuiWidth - SEARCH_WIDTH) / 2;
                int sy = SEARCH_Y;
                Theme.drawRoundedPanel(graphics, sx, sy, SEARCH_WIDTH, SEARCH_HEIGHT);
                boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
                if (search.isEmpty()) {
                        if (blink) {
                                graphics.fill(sx + 8, sy + 4, sx + 9, sy + 12, Theme.textDim());
                        }
                        graphics.drawString(font, "Search modules...", sx + 12, sy + 4, Theme.textDim());
                } else {
                        graphics.drawString(font, search, sx + 8, sy + 4, Theme.white());
                        if (blink) {
                                int cx = sx + 9 + font.width(search);
                                graphics.fill(cx, sy + 4, cx + 1, sy + 12, Theme.white());
                        }
                }
        }

        /** Flow layout: panels left to right, wrapping into rows; settings panel last. */
        private void layout() {
                int columns = Math.max(1, (lastGuiWidth - PANEL_MARGIN * 2 + PANEL_GAP) / (Theme.PANEL_WIDTH + PANEL_GAP));
                int x = PANEL_MARGIN;
                int y = PANEL_START_Y;
                int rowMax = 0;
                int col = 0;
                for (Panel panel : panels) {
                        if (col >= columns) {
                                x = PANEL_MARGIN;
                                y += rowMax + PANEL_GAP;
                                col = 0;
                                rowMax = 0;
                        }
                        panel.setPosition(x, y);
                        rowMax = Math.max(rowMax, panel.baseHeight());
                        x += Theme.PANEL_WIDTH + PANEL_GAP;
                        col++;
                }
                if (col >= columns) {
                        x = PANEL_MARGIN;
                        y += rowMax + PANEL_GAP;
                }
                settingsPanel.setPosition(x, y);
        }

        private void applyFilter() {
                String query = search.trim();
                for (Panel panel : panels) {
                        panel.setFilter(query);
                }
        }

        private void startClose() {
                closing = true;
        }

        // ---------------------------------------------------------------- keyboard

        @Override
        public boolean keyPressed(KeyEvent event) {
                if (closing) {
                        return true;
                }
                int key = event.key();
                // keybind capture gets every key first (ESC cancels, Backspace clears)
                for (Panel panel : panels) {
                        if (panel.keyPressed(key)) {
                                return true;
                        }
                }
                if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                        startClose();
                        return true;
                }
                if (key == GLFW.GLFW_KEY_BACKSPACE) {
                        if (!search.isEmpty()) {
                                search = search.substring(0, search.length() - 1);
                                applyFilter();
                        }
                        return true;
                }
                if (key >= 32 && key < 127) {
                        char c = (char) key;
                        if (c >= 'A' && c <= 'Z') {
                                c = (char) (c + 32);
                        }
                        if (search.length() < 24) {
                                search = search + c;
                                applyFilter();
                        }
                        return true;
                }
                return super.keyPressed(event);
        }

        // ---------------------------------------------------------------- mouse

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
                if (closing) {
                        return true;
                }
                double mx = event.x();
                double my = event.y();
                int button = event.button();
                int sx = (lastGuiWidth - SEARCH_WIDTH) / 2;
                if (button == 0 && mx >= sx && mx < sx + SEARCH_WIDTH && my >= SEARCH_Y && my < SEARCH_Y + SEARCH_HEIGHT) {
                        for (Panel panel : panels) {
                                panel.cancelAllCaptures();
                        }
                        return true;
                }
                if (settingsPanel.mouseClicked(mx, my, button)) {
                        return true;
                }
                for (int i = panels.size() - 1; i >= 0; i--) {
                        if (panels.get(i).mouseClicked(mx, my, button)) {
                                return true;
                        }
                }
                return super.mouseClicked(event, doubled);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
                if (closing) {
                        return true;
                }
                double mx = event.x();
                double my = event.y();
                if (settingsPanel.mouseDragged(mx, my)) {
                        return true;
                }
                for (Panel panel : panels) {
                        if (panel.mouseDragged(mx, my)) {
                                return true;
                        }
                }
                return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
                double mx = event.x();
                double my = event.y();
                settingsPanel.mouseReleased(mx, my, event.button());
                for (Panel panel : panels) {
                        panel.mouseReleased(mx, my, event.button());
                }
                return super.mouseReleased(event);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                if (closing) {
                        return true;
                }
                for (int i = panels.size() - 1; i >= 0; i--) {
                        if (panels.get(i).mouseScrolled(mouseX, mouseY, (float) scrollY)) {
                                return true;
                        }
                }
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // ---------------------------------------------------------------- lifecycle

        @Override
        public void onClose() {
                ConfigManager.saveProfile(ConfigManager.activeProfile());
                Minecraft.getInstance().setScreen(null);
        }

        /** First free "profile_N" name based on the files in the profiles dir. */
        private static String nextProfileName() {
                Path dir = ConfigManager.profilesDir();
                int max = 0;
                try {
                        if (Files.isDirectory(dir)) {
                                try (var stream = Files.list(dir)) {
                                        for (Path file : stream.toList()) {
                                                String name = file.getFileName().toString();
                                                if (name.startsWith("profile_") && name.endsWith(".json")) {
                                                        try {
                                                                max = Math.max(max, Integer.parseInt(name.substring(8, name.length() - 5)));
                                                        } catch (NumberFormatException ignored) {
                                                        }
                                                }
                                        }
                                }
                        }
                } catch (IOException e) {
                        ConfigManager.RydonutLogger.warn("Could not scan profiles dir: " + e);
                        return "profile_" + (System.currentTimeMillis() % 100000L);
                }
                return "profile_" + (max + 1);
        }

        /**
         * Fifth panel: profile actions (operating on the active profile), a
         * "New Profile" button and a shortcut into the HUD editor.
         */
        private final class SettingsPanel {
                private static final String[] ACTIONS = {"Save", "Load", "Export", "Import", "New Profile", "HUD Editor"};

                private int x;
                private int y;
                private boolean draggingHeader;
                private double dragOffX;
                private double dragOffY;
                private String status;
                private long statusUntil;

                private void setPosition(int x, int y) {
                        this.x = x;
                        this.y = y;
                }

                private int baseHeight() {
                        return Theme.TITLE_HEIGHT + ACTIONS.length * Theme.ITEM_HEIGHT + Theme.PADDING * 2;
                }

                private void setStatus(String text) {
                        status = text;
                        statusUntil = System.currentTimeMillis() + 2000L;
                }

                private void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
                        int w = Theme.PANEL_WIDTH;
                        int h = baseHeight();
                        Theme.drawRoundedPanel(graphics, x, y, w, h);

                        boolean showStatus = status != null && System.currentTimeMillis() < statusUntil;
                        graphics.drawString(font, "Settings", x + 8, y + 7, Theme.white());
                        String right = showStatus ? status : "Profile: " + ConfigManager.activeProfile();
                        right = Theme.ellipsize(font, right, w - 16 - font.width("Settings"));
                        graphics.drawString(font, right, x + w - 8 - font.width(right), y + 7, showStatus ? Theme.success() : Theme.textDim());

                        graphics.fill(x + 2, y + Theme.TITLE_HEIGHT - 1, x + w - 2, y + Theme.TITLE_HEIGHT, Theme.outline());

                        int cy = y + Theme.TITLE_HEIGHT + Theme.PADDING;
                        for (int i = 0; i < ACTIONS.length; i++) {
                                boolean hover = mouseX >= x + 2 && mouseX < x + w - 2 && mouseY >= cy && mouseY < cy + Theme.ITEM_HEIGHT;
                                if (hover) {
                                        graphics.fill(x + 2, cy, x + w - 2, cy + Theme.ITEM_HEIGHT, Theme.hoveredRow());
                                }
                                int color;
                                if (hover) {
                                        color = Theme.white();
                                } else {
                                        color = i == ACTIONS.length - 1 ? Theme.accent() : Theme.textDim();
                                }
                                graphics.drawString(font, ACTIONS[i], x + 10, cy + 5, color);
                                cy += Theme.ITEM_HEIGHT;
                        }
                }

                private boolean mouseClicked(double mx, double my, int button) {
                        if (mx < x || mx >= x + Theme.PANEL_WIDTH) {
                                return false;
                        }
                        if (my >= y && my < y + Theme.TITLE_HEIGHT) {
                                if (button == 0) {
                                        draggingHeader = true;
                                        dragOffX = mx - x;
                                        dragOffY = my - y;
                                }
                                return true;
                        }
                        int cy = y + Theme.TITLE_HEIGHT + Theme.PADDING;
                        for (int i = 0; i < ACTIONS.length; i++) {
                                if (my >= cy && my < cy + Theme.ITEM_HEIGHT) {
                                        if (button == 0) {
                                                run(i);
                                        }
                                        return true;
                                }
                                cy += Theme.ITEM_HEIGHT;
                        }
                        return false;
                }

                private void run(int action) {
                        String profile = ConfigManager.activeProfile();
                        switch (action) {
                                case 0 -> {
                                        ConfigManager.saveProfile(profile);
                                        setStatus("Saved");
                                }
                                case 1 -> {
                                        ConfigManager.loadProfile(profile);
                                        setStatus("Loaded");
                                }
                                case 2 -> {
                                        ConfigManager.exportProfile(profile);
                                        setStatus("Exported");
                                }
                                case 3 -> {
                                        ConfigManager.importLatestExport(profile);
                                        setStatus("Imported");
                                }
                                case 4 -> {
                                        String name = nextProfileName();
                                        ConfigManager.newProfile(name);
                                        setStatus("New: " + name);
                                }
                                default -> Minecraft.getInstance().setScreen(new HudEditorScreen());
                        }
                }

                private boolean mouseDragged(double mx, double my) {
                        if (!draggingHeader) {
                                return false;
                        }
                        x = Mth.clamp((int) (mx - dragOffX), 0, Math.max(0, lastGuiWidth - Theme.PANEL_WIDTH));
                        y = Mth.clamp((int) (my - dragOffY), 0, Math.max(0, lastGuiHeight - baseHeight()));
                        return true;
                }

                private void mouseReleased(double mx, double my, int button) {
                        draggingHeader = false;
                }
        }
}
