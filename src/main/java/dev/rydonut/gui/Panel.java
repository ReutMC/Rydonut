package dev.rydonut.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.gui.widgets.SettingWidget;
import dev.rydonut.render.Colors;
import dev.rydonut.settings.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * One category panel of the ClickGUI: draggable title bar with a collapse
 * button, module rows (left-click expands the settings, right-click toggles
 * the module) and a clipped, scrollable settings area. With an active search
 * filter the body lists only matching modules, auto-expanded.
 *
 * <p>All coordinates are logical screen coordinates; the slide animation of
 * the owning screen is applied via the graphics pose, so widget geometry
 * recorded here stays valid for hit tests at all times.
 */
public class Panel {
        private final String title;
        private final List<Module> modules;

        private int x;
        private int y;

        private boolean expanded = true;
        private float openAnim = 1.0F;

        private Module expandedModule;
        private float scroll;

        private boolean draggingHeader;
        private double dragOffX;
        private double dragOffY;

        private String filter = "";

        private final Map<Module, List<SettingWidget>> widgetCache = new HashMap<>();

        // geometry captured during render, reused for hit testing
        private int lastBodyTop;
        private int lastVisBodyH;
        private int lastNaturalBodyH;
        private int lastGuiW;
        private int lastGuiH;

        public Panel(Category category) {
                this(category.title, ModuleManager.get().getModulesIn(category));
        }

        public Panel(String title, List<Module> modules) {
                this.title = title;
                this.modules = new ArrayList<>(modules);
        }

        // ---------------------------------------------------------------- layout

        public void setPosition(int x, int y) {
                this.x = x;
                this.y = y;
        }

        public int getX() {
                return x;
        }

        public int getY() {
                return y;
        }

        /** Height with every module collapsed; used by the screen flow layout. */
        public int baseHeight() {
                return Theme.TITLE_HEIGHT + modules.size() * Theme.ITEM_HEIGHT + Theme.PADDING * 2;
        }

        public void setFilter(String query) {
                this.filter = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
                this.scroll = 0.0F;
        }

        // ---------------------------------------------------------------- helpers

        private boolean filtering() {
                return !filter.isEmpty();
        }

        private List<Module> matched() {
                List<Module> out = new ArrayList<>();
                for (Module module : modules) {
                        if (module.getName().toLowerCase(Locale.ROOT).contains(filter)) {
                                out.add(module);
                        }
                }
                return out;
        }

        private List<SettingWidget> widgetsFor(Module module) {
                return widgetCache.computeIfAbsent(module, mod -> {
                        List<SettingWidget> list = new ArrayList<>();
                        for (Setting<?> s : mod.getSettings()) {
                                if (s.getName().startsWith("#")) {
                                        continue; // hidden internals (e.g. HudModule position adapter)
                                }
                                list.add(SettingWidget.create(s));
                        }
                        return list;
                });
        }

        private int settingsHeight(Module module) {
                int height = 0;
                for (SettingWidget widget : widgetsFor(module)) {
                        if (widget.setting().isVisible()) {
                                height += widget.getHeight();
                        }
                }
                return height;
        }

        private int naturalBodyHeight() {
                int height = Theme.PADDING;
                if (filtering()) {
                        List<Module> matches = matched();
                        if (matches.isEmpty()) {
                                height += Theme.ITEM_HEIGHT;
                        } else {
                                for (Module module : matches) {
                                        height += Theme.ITEM_HEIGHT + settingsHeight(module);
                                }
                        }
                } else {
                        height += modules.size() * Theme.ITEM_HEIGHT;
                        if (expandedModule != null) {
                                height += settingsHeight(expandedModule);
                        }
                }
                return height + Theme.PADDING;
        }

        private List<SettingWidget> currentWidgets() {
                List<SettingWidget> out = new ArrayList<>();
                if (filtering()) {
                        for (Module module : matched()) {
                                out.addAll(widgetsFor(module));
                        }
                } else if (expandedModule != null) {
                        out.addAll(widgetsFor(expandedModule));
                }
                return out;
        }

        private int enabledCount() {
                int count = 0;
                for (Module module : modules) {
                        if (module.isEnabled()) {
                                count++;
                        }
                }
                return count;
        }

        // ---------------------------------------------------------------- render

        public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
                lastGuiW = graphics.guiWidth();
                lastGuiH = graphics.guiHeight();
                openAnim = Theme.approach(openAnim, expanded ? 1.0F : 0.0F, delta);

                int width = Theme.PANEL_WIDTH;
                int naturalBody = naturalBodyHeight();
                lastNaturalBodyH = naturalBody;
                int visBody = Math.round(naturalBody * openAnim);
                int maxBody = Math.max(0, lastGuiH - (y + Theme.TITLE_HEIGHT) - 6);
                visBody = Mth.clamp(visBody, 0, maxBody);
                lastBodyTop = y + Theme.TITLE_HEIGHT;
                lastVisBodyH = visBody;

                Theme.drawRoundedPanel(graphics, x, y, width, Theme.TITLE_HEIGHT + visBody);

                // ---- title bar
                boolean headerHover = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + Theme.TITLE_HEIGHT;
                if (headerHover) {
                        graphics.fill(x + 2, y + 1, x + width - 2, y + Theme.TITLE_HEIGHT - 1, Colors.withAlpha(Theme.panelLight(), 0.55F));
                }
                graphics.drawString(font, title, x + 8, y + 7, Theme.white());
                String count = enabledCount() + "/" + modules.size();
                graphics.drawString(font, count, x + width - 24 - font.width(count), y + 7, Theme.textDim());

                int gx = x + width - 18;
                int gy = y + 5;
                if (mouseX >= gx && mouseX < gx + 14 && mouseY >= gy && mouseY < gy + 12) {
                        graphics.fill(gx, gy, gx + 14, gy + 12, Theme.panelLight());
                }
                graphics.fill(gx + 4, gy + 6, gx + 10, gy + 7, Theme.white());
                if (!expanded) {
                        graphics.fill(gx + 6, gy + 3, gx + 7, gy + 9, Theme.white());
                }
                graphics.fill(x + 2, y + Theme.TITLE_HEIGHT - 1, x + width - 2, y + Theme.TITLE_HEIGHT, Theme.outline());

                // ---- body
                float maxScroll = Math.max(0.0F, naturalBody - visBody);
                scroll = Mth.clamp(scroll, 0.0F, maxScroll);
                if (visBody > 0) {
                        graphics.enableScissor(x + 1, lastBodyTop, x + width - 1, lastBodyTop + visBody);
                        int cy = lastBodyTop + Theme.PADDING - (int) scroll;
                        if (filtering()) {
                                List<Module> matches = matched();
                                if (matches.isEmpty()) {
                                        graphics.drawString(font, "No matches", x + 12, cy + 5, Theme.textDim());
                                } else {
                                        for (Module module : matches) {
                                                cy = renderModule(graphics, font, module, cy, mouseX, mouseY, delta, true);
                                        }
                                }
                        } else {
                                for (Module module : modules) {
                                        cy = renderModule(graphics, font, module, cy, mouseX, mouseY, delta, false);
                                }
                        }
                        graphics.disableScissor();

                        if (maxScroll > 0.0F) {
                                int trackH = visBody - 4;
                                float ratio = (float) visBody / naturalBody;
                                int thumbH = Math.max(10, (int) (trackH * ratio));
                                int thumbY = lastBodyTop + 2 + (int) ((trackH - thumbH) * (scroll / maxScroll));
                                graphics.fill(x + width - 4, lastBodyTop + 2, x + width - 2, lastBodyTop + 2 + trackH, 0x33000000);
                                graphics.fill(x + width - 4, thumbY, x + width - 2, thumbY + thumbH, Colors.withAlpha(Theme.primary(), 0.7F));
                        }
                }
        }

        private int renderModule(GuiGraphics graphics, Font font, Module module, int cy, int mouseX, int mouseY, float delta, boolean forceExpanded) {
                int width = Theme.PANEL_WIDTH;
                boolean rowHover = mouseX >= x + 2 && mouseX < x + width - 2 && mouseY >= cy && mouseY < cy + Theme.ITEM_HEIGHT;
                if (rowHover) {
                        graphics.fill(x + 2, cy, x + width - 2, cy + Theme.ITEM_HEIGHT, Theme.hoveredRow());
                }
                if (module.isEnabled()) {
                        graphics.fill(x + 2, cy + 3, x + 5, cy + Theme.ITEM_HEIGHT - 3, module.getListColor());
                }
                graphics.drawString(font, module.getName(), x + 12, cy + 5, module.isEnabled() ? Theme.white() : Theme.textDim());

                boolean showChildren = forceExpanded || module == expandedModule;
                boolean hasSettings = settingsHeight(module) > 0;
                if (hasSettings) {
                        graphics.drawString(font, showChildren ? "v" : ">", x + width - 14, cy + 5, Theme.textDim());
                }
                cy += Theme.ITEM_HEIGHT;
                if (showChildren && hasSettings) {
                        for (SettingWidget widget : widgetsFor(module)) {
                                if (!widget.setting().isVisible()) {
                                        continue;
                                }
                                cy += widget.render(graphics, font, x + 4, cy, width - 8, mouseX, mouseY, delta);
                        }
                }
                return cy;
        }

        // ---------------------------------------------------------------- input

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
                int width = Theme.PANEL_WIDTH;
                if (mouseX < x || mouseX >= x + width) {
                        return false;
                }

                if (mouseY >= y && mouseY < y + Theme.TITLE_HEIGHT) {
                        if (button == 0) {
                                int gx = x + width - 18;
                                int gy = y + 5;
                                if (mouseX >= gx && mouseX < gx + 14 && mouseY >= gy && mouseY < gy + 12) {
                                        expanded = !expanded;
                                } else {
                                        draggingHeader = true;
                                        dragOffX = mouseX - x;
                                        dragOffY = mouseY - y;
                                }
                        }
                        return true;
                }

                if (openAnim < 0.5F || lastVisBodyH <= 0) {
                        return false;
                }
                if (mouseY < lastBodyTop || mouseY >= lastBodyTop + lastVisBodyH) {
                        return false;
                }

                if (button == 0) {
                        // a click somewhere else ends an active keybind capture
                        for (SettingWidget widget : currentWidgets()) {
                                if (widget.isCapturing() && !widget.inBounds(mouseX, mouseY)) {
                                        widget.cancelCapture();
                                }
                        }
                }

                int cy = lastBodyTop + Theme.PADDING - (int) scroll;
                if (filtering()) {
                        for (Module module : matched()) {
                                cy = clickModule(module, cy, mouseX, mouseY, button, true);
                                if (cy < 0) {
                                        return true;
                                }
                        }
                } else {
                        for (Module module : modules) {
                                cy = clickModule(module, cy, mouseX, mouseY, button, false);
                                if (cy < 0) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        /** Returns the next content y, or -1 when the click was consumed. */
        private int clickModule(Module module, int cy, double mouseX, double mouseY, int button, boolean forceExpanded) {
                if (mouseY >= cy && mouseY < cy + Theme.ITEM_HEIGHT) {
                        if (button == 0) {
                                if (filtering()) {
                                        module.toggle();
                                        ConfigManager.markDirty();
                                } else {
                                        expandedModule = expandedModule == module ? null : module;
                                        scroll = 0.0F;
                                }
                        } else if (button == 1) {
                                module.toggle();
                                ConfigManager.markDirty();
                        }
                        return -1;
                }
                cy += Theme.ITEM_HEIGHT;
                boolean showChildren = forceExpanded || module == expandedModule;
                if (showChildren) {
                        for (SettingWidget widget : widgetsFor(module)) {
                                if (!widget.setting().isVisible()) {
                                        continue;
                                }
                                if (widget.inBounds(mouseX, mouseY) && widget.mouseClicked(mouseX, mouseY, button)) {
                                        return -1;
                                }
                                cy += widget.getHeight();
                        }
                }
                return cy;
        }

        public boolean mouseDragged(double mouseX, double mouseY) {
                if (draggingHeader) {
                        x = Mth.clamp((int) (mouseX - dragOffX), 0, Math.max(0, lastGuiW - Theme.PANEL_WIDTH));
                        y = Mth.clamp((int) (mouseY - dragOffY), 0, Math.max(0, lastGuiH - Theme.TITLE_HEIGHT - 20));
                        return true;
                }
                for (SettingWidget widget : currentWidgets()) {
                        if (widget.mouseDragged(mouseX, mouseY)) {
                                return true;
                        }
                }
                return false;
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
                draggingHeader = false;
                for (SettingWidget widget : currentWidgets()) {
                        widget.mouseReleased(mouseX, mouseY, button);
                }
        }

        public boolean mouseScrolled(double mouseX, double mouseY, float scrollY) {
                if (mouseX < x || mouseX >= x + Theme.PANEL_WIDTH) {
                        return false;
                }
                if (mouseY < lastBodyTop || mouseY >= lastBodyTop + lastVisBodyH) {
                        return false;
                }
                for (SettingWidget widget : currentWidgets()) {
                        if (widget.mouseScrolled(mouseX, mouseY, scrollY)) {
                                return true;
                        }
                }
                float maxScroll = Math.max(0.0F, lastNaturalBodyH - lastVisBodyH);
                if (maxScroll > 0.0F) {
                        scroll = Mth.clamp(scroll - scrollY * 16.0F, 0.0F, maxScroll);
                        return true;
                }
                return false;
        }

        public boolean keyPressed(int keyCode) {
                for (SettingWidget widget : currentWidgets()) {
                        if (widget.keyPressed(keyCode)) {
                                return true;
                        }
                }
                return false;
        }

        public void cancelAllCaptures() {
                for (SettingWidget widget : currentWidgets()) {
                        widget.cancelCapture();
                }
        }
}
