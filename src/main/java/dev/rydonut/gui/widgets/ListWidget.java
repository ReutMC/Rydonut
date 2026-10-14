package dev.rydonut.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.gui.Theme;
import dev.rydonut.render.Colors;
import dev.rydonut.settings.StringListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Editable string list (block/item id lists). Collapsed shows the entry
 * count; expanded shows a scrollable entry list with per-entry remove
 * buttons, preset toggle chips (quick add/remove) and a "Clear all" row.
 *
 * <p>Chip flow layout is computed against the widget width the panel always
 * uses ({@code Theme.PANEL_WIDTH - 8}), so {@link #getHeight()} stays exact.
 */
public class ListWidget extends SettingWidget {
        private static final String[] PRESETS = {
                        "minecraft:torch", "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
                        "minecraft:barrel", "minecraft:shulker_box", "minecraft:obsidian", "minecraft:cobblestone",
                        "minecraft:furnace", "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser",
                        "minecraft:spawner", "minecraft:jukebox", "minecraft:crafting_table"
        };
        private static final int ENTRY_HEIGHT = 13;
        private static final int CHIP_HEIGHT = 13;
        private static final int CHIP_GAP = 3;
        private static final int CHIP_ROW = CHIP_HEIGHT + 2;
        private static final int MAX_LIST_HEIGHT = 90;
        private static final int CLEAR_ROW_HEIGHT = 14;
        private static final int LAYOUT_WIDTH = Theme.PANEL_WIDTH - 8;

        private final StringListSetting list;
        private boolean expanded;
        private float listScroll;
        private int listTop;
        private int listHeight;
        /** Chip rects from the last render: {x, y, width, presetIndex}. */
        private final List<int[]> chipRects = new ArrayList<>();

        public ListWidget(StringListSetting setting) {
                super(setting);
                this.list = setting;
        }

        private static Font font() {
                return Minecraft.getInstance().font;
        }

        private static String shortName(String id) {
                int i = id.indexOf(':');
                return i >= 0 ? id.substring(i + 1) : id;
        }

        private int chipRowCount(Font font, int width) {
                int rows = 1;
                int cursor = Theme.PADDING;
                for (String preset : PRESETS) {
                        int w = font.width(shortName(preset)) + 10;
                        if (cursor + w > width - Theme.PADDING && cursor > Theme.PADDING) {
                                rows++;
                                cursor = Theme.PADDING;
                        }
                        cursor += w + CHIP_GAP;
                }
                return rows;
        }

        @Override
        public int getHeight() {
                if (!expanded) {
                        return Theme.ITEM_HEIGHT;
                }
                int entries = list.get().size();
                int listH = Math.min(MAX_LIST_HEIGHT, entries * ENTRY_HEIGHT);
                int chips = chipRowCount(font(), LAYOUT_WIDTH) * CHIP_ROW;
                return Theme.ITEM_HEIGHT + (listH > 0 ? listH + 3 : 0) + chips + CLEAR_ROW_HEIGHT;
        }

        @Override
        public int render(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY, float delta) {
                track(x, y, width, getHeight(), mouseX, mouseY);
                if (hovered) {
                        graphics.fill(x + 2, y, x + width - 2, y + Theme.ITEM_HEIGHT, Theme.hoveredRow());
                }
                String header = list.getName() + " (" + list.get().size() + ")";
                graphics.drawString(font, header, x + Theme.PADDING, y + 5, hovered ? Theme.white() : Theme.textDim());
                graphics.drawString(font, expanded ? "v" : ">", x + width - 12, y + 5, Theme.textDim());

                int cy = y + Theme.ITEM_HEIGHT;
                if (expanded) {
                        // ---- entry list (clipped + scrollable)
                        int entries = list.get().size();
                        listHeight = Math.min(MAX_LIST_HEIGHT, entries * ENTRY_HEIGHT);
                        listTop = cy;
                        if (listHeight > 0) {
                                float maxScroll = Math.max(0.0F, entries * ENTRY_HEIGHT - listHeight);
                                listScroll = Mth.clamp(listScroll, 0.0F, maxScroll);
                                graphics.enableScissor(x + 2, listTop, x + width - 2, listTop + listHeight);
                                int ey = listTop - (int) listScroll;
                                for (String entry : list.get()) {
                                        int rowTop = Math.max(ey, listTop);
                                        int rowBottom = Math.min(ey + ENTRY_HEIGHT, listTop + listHeight);
                                        boolean rowHover = mouseX >= x + 2 && mouseX < x + width - 2 && mouseY >= rowTop && mouseY < rowBottom;
                                        if (rowHover && rowBottom > rowTop) {
                                                graphics.fill(x + 2, rowTop, x + width - 2, rowBottom, Theme.hoveredRow());
                                        }
                                        graphics.drawString(font, Theme.ellipsize(font, entry, width - 30), x + 6, ey + 3, Theme.textDim());
                                        boolean xHover = rowHover && mouseX >= x + width - 14 && mouseX < x + width - 4;
                                        if (xHover) {
                                                graphics.fill(x + width - 14, ey + 2, x + width - 4, ey + 12, Colors.withAlpha(Theme.danger(), 0.30F));
                                        }
                                        graphics.drawString(font, "x", x + width - 11, ey + 3, xHover ? Theme.danger() : Theme.textDim());
                                        ey += ENTRY_HEIGHT;
                                }
                                graphics.disableScissor();
                                // thin frame around the list
                                graphics.fill(x + 2, listTop, x + width - 2, listTop + 1, Theme.outline());
                                graphics.fill(x + 2, listTop + listHeight - 1, x + width - 2, listTop + listHeight, Theme.outline());
                                graphics.fill(x + 2, listTop, x + 3, listTop + listHeight, Theme.outline());
                                graphics.fill(x + width - 3, listTop, x + width - 2, listTop + listHeight, Theme.outline());
                                cy += listHeight + 3;
                        }

                        // ---- preset chips
                        chipRects.clear();
                        int cursor = Theme.PADDING;
                        int row = 0;
                        for (int i = 0; i < PRESETS.length; i++) {
                                String label = shortName(PRESETS[i]);
                                int w = font.width(label) + 10;
                                if (cursor + w > width - Theme.PADDING && cursor > Theme.PADDING) {
                                        row++;
                                        cursor = Theme.PADDING;
                                }
                                int cx = x + cursor;
                                int cyy = cy + row * CHIP_ROW;
                                boolean on = list.contains(PRESETS[i]);
                                boolean chipHover = mouseX >= cx && mouseX < cx + w && mouseY >= cyy && mouseY < cyy + CHIP_HEIGHT;
                                int bg = on ? Colors.lerp(Theme.panelLight(), Theme.primary(), 0.30F) : Theme.panelLight();
                                graphics.fill(cx + 1, cyy, cx + w - 1, cyy + CHIP_HEIGHT, bg);
                                graphics.fill(cx, cyy + 1, cx + w, cyy + CHIP_HEIGHT - 1, bg);
                                graphics.drawString(font, label, cx + 5, cyy + 3, on || chipHover ? Theme.white() : Theme.textDim());
                                chipRects.add(new int[] {cx, cyy, w, i});
                                cursor += w + CHIP_GAP;
                        }
                        cy += chipRowCount(font, width) * CHIP_ROW;

                        // ---- clear all row
                        boolean clearHover = mouseX >= x + 2 && mouseX < x + width - 2 && mouseY >= cy && mouseY < cy + CLEAR_ROW_HEIGHT;
                        if (clearHover) {
                                graphics.fill(x + 2, cy, x + width - 2, cy + CLEAR_ROW_HEIGHT, Colors.withAlpha(Theme.danger(), 0.12F));
                        }
                        graphics.drawString(font, "Clear all", x + Theme.PADDING, cy + 3, clearHover ? Theme.danger() : Theme.textDim());
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
                        listScroll = 0.0F;
                        return true;
                }
                if (!expanded) {
                        return false;
                }
                // entry remove buttons
                if (listHeight > 0 && mouseX >= lastX + lastWidth - 14 && mouseX < lastX + lastWidth - 4
                                && mouseY >= listTop && mouseY < listTop + listHeight) {
                        int index = (int) ((mouseY - (listTop - listScroll)) / ENTRY_HEIGHT);
                        List<String> entries = list.get();
                        if (index >= 0 && index < entries.size()) {
                                list.remove(entries.get(index));
                                ConfigManager.markDirty();
                                return true;
                        }
                }
                // preset chips
                for (int[] rect : chipRects) {
                        if (mouseX >= rect[0] && mouseX < rect[0] + rect[2] && mouseY >= rect[1] && mouseY < rect[1] + CHIP_HEIGHT) {
                                list.toggle(PRESETS[rect[3]]);
                                ConfigManager.markDirty();
                                return true;
                        }
                }
                // clear all row
                int clearY = lastY + getHeight() - CLEAR_ROW_HEIGHT;
                if (mouseY >= clearY && mouseY < clearY + CLEAR_ROW_HEIGHT) {
                        list.set(new ArrayList<>());
                        ConfigManager.markDirty();
                        return true;
                }
                return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, float scrollY) {
                if (!expanded || listHeight <= 0) {
                        return false;
                }
                if (mouseX < lastX || mouseX >= lastX + lastWidth || mouseY < listTop || mouseY >= listTop + listHeight) {
                        return false;
                }
                float maxScroll = Math.max(0.0F, list.get().size() * ENTRY_HEIGHT - listHeight);
                if (maxScroll <= 0.0F) {
                        return false;
                }
                listScroll = Mth.clamp(listScroll - scrollY * ENTRY_HEIGHT, 0.0F, maxScroll);
                return true;
        }
}
