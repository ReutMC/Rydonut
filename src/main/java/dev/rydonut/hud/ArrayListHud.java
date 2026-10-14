package dev.rydonut.hud;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.core.Module;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.render.Colors;

/**
 * Animated module list (ArrayList) on the right side of the screen.
 * Modules slide in on enable and slide out on disable; sorted by text width.
 */
public class ArrayListHud extends HudModule {
        private static final class Row {
                final String name;
                float offset = 1.2f; // 0 = settled, 1+ = off

                Row(String name) {
                        this.name = name;
                }
        }

        private final List<Row> rows = new ArrayList<>();

        public ArrayListHud() {
                super("ArrayList", "Animated enabled-modules list.");
                setPosition(-1, 6);
        }

        @Override
        public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
                var font = mc.font;
                List<Module> active = ModuleManager.get().getActiveSortedByWidth();

                // sync rows with modules (keep out-animation state for disabled ones)
                List<Row> synced = new ArrayList<>();
                for (Module module : active) {
                        Row row = findRow(module.getName());
                        if (row == null) {
                                row = new Row(module.getName());
                        }
                        synced.add(row);
                }
                for (Row row : rows) {
                        if (findIn(synced, row.name) == null) {
                                synced.add(row); // animate out
                        }
                }
                rows.clear();
                rows.addAll(synced);

                int lineH = font.lineHeight + 2;
                int right = getX() < 0 ? graphics.guiWidth() - 4 : getX() + getWidth();
                int y = getY();
                int widest = 0;

                // animate + measure
                List<Row> visible = new ArrayList<>();
                for (Row row : rows) {
                        boolean enabledNow = isActive(row.name);
                        row.offset += ((enabledNow ? 0.0f : 1.2f) - row.offset) * 0.16f;
                        if (row.offset > 1.0f) {
                                continue; // fully slid out
                        }
                        visible.add(row);
                        widest = Math.max(widest, font.width(row.name));
                }
                visible.sort((a, b) -> font.width(b.name) - font.width(a.name));

                setSize(widest + 10, lineH * Math.max(1, visible.size()));

                for (int i = 0; i < visible.size(); i++) {
                        Row row = visible.get(i);
                        int slide = (int) (row.offset * 40);
                        int textW = font.width(row.name);
                        int x = right - (textW + 8) + slide;
                        float t = i / Math.max(1.0f, visible.size() - 1.0f);
                        int color = Colors.lerp(Colors.PRIMARY, Colors.ACCENT, t);
                        graphics.fill(x, y, right, y + lineH, 0xA60D1B2A);
                        graphics.fill(right - 1, y, right, y + lineH, color);
                        graphics.drawString(font, row.name, x + 4, y + 1, Colors.WHITE);
                        y += lineH;
                }
        }

        private boolean isActive(String name) {
                Module module = ModuleManager.get().getByName(name);
                return module != null && module.isEnabled();
        }

        private Row findRow(String name) {
                for (Row row : rows) {
                        if (row.name.equals(name)) {
                                return row;
                        }
                }
                return null;
        }

        private static Row findIn(List<Row> list, String name) {
                if (list == null) {
                        return null;
                }
                for (Row row : list) {
                        if (row.name.equals(name)) {
                                return row;
                        }
                }
                return null;
        }
}
