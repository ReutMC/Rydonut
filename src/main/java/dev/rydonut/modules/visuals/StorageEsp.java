package dev.rydonut.modules.visuals;

import java.util.Map;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Storage ESP: chests, trapped/ender chests, barrels, shulkers, hoppers,
 * furnaces, droppers/dispensers with per-type colors and labels.
 */
public class StorageEsp extends Module {
        private final DoubleSetting range = new DoubleSetting("Range", 48, 8, 128, 8);
        private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
        private final BoolSetting showLabels = new BoolSetting("Labels", true);
        private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.2, 0.05, 1.0, 0.05);

        private final BoolSetting chests = new BoolSetting("Chests", true);
        private final BoolSetting enderChests = new BoolSetting("Ender chests", true);
        private final BoolSetting barrels = new BoolSetting("Barrels", true);
        private final BoolSetting shulkers = new BoolSetting("Shulkers", true);
        private final BoolSetting hoppers = new BoolSetting("Hoppers", true);
        private final BoolSetting furnaces = new BoolSetting("Furnaces", true);
        private final BoolSetting droppers = new BoolSetting("Droppers/Dispensers", true);

        private final ColorSetting chestColor = new ColorSetting("Chest color", 0xFFFFC04D);
        private final ColorSetting enderColor = new ColorSetting("Ender color", 0xFFB465DA);
        private final ColorSetting barrelColor = new ColorSetting("Barrel color", 0xFFD2955B);
        private final ColorSetting shulkerColor = new ColorSetting("Shulker color", 0xFFB072E0);
        private final ColorSetting hopperColor = new ColorSetting("Hopper color", 0xFF9AA5B1);
        private final ColorSetting furnaceColor = new ColorSetting("Furnace color", 0xFF8D8D8D);
        private final ColorSetting dropperColor = new ColorSetting("Dropper color", 0xFF6E7B8B);

        public StorageEsp() {
                super("Storage ESP", Category.VISUALS, "Highlights containers.");
                addSettings(range, throughWalls, showLabels, fillOpacity,
                                chests, enderChests, barrels, shulkers, hoppers, furnaces, droppers,
                                chestColor, enderColor, barrelColor, shulkerColor, hopperColor, furnaceColor, dropperColor);
        }

        @Override
        public void onWorldRender(WorldRenderContext context) {
                if (mc.player == null || mc.level == null) {
                        return;
                }
                WorldShapes.begin(context);
                try {
                        int range = (int) Math.ceil(this.range.get() / 16.0);
                        BlockPos playerPos = mc.player.blockPosition();
                        ChunkPos center = mc.player.chunkPosition();
                        for (int dx = -range; dx <= range; dx++) {
                                for (int dz = -range; dz <= range; dz++) {
                                        LevelChunk chunk = mc.level.getChunkSource().getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false);
                                        if (chunk == null) {
                                                continue;
                                        }
                                        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                                                BlockPos pos = entry.getKey();
                                                if (pos.distSqr(playerPos) > this.range.get() * this.range.get()) {
                                                        continue;
                                                }
                                                renderBlockEntity(pos, entry.getValue());
                                        }
                                }
                        }
                } finally {
                        WorldShapes.end();
                }
        }

        private void renderBlockEntity(BlockPos pos, BlockEntity be) {
                boolean tw = throughWalls.isOn();
                BlockState state = be.getBlockState();
                int color;
                String label;
                if (state.is(Blocks.TRAPPED_CHEST)) {
                        if (!chests.isOn()) return;
                        color = chestColor.get();
                        label = "Trapped chest";
                } else if (state.is(Blocks.CHEST)) {
                        if (!chests.isOn()) return;
                        color = chestColor.get();
                        label = "Chest";
                } else if (state.is(Blocks.ENDER_CHEST)) {
                        if (!enderChests.isOn()) return;
                        color = enderColor.get();
                        label = "Ender chest";
                } else if (state.is(Blocks.BARREL)) {
                        if (!barrels.isOn()) return;
                        color = barrelColor.get();
                        label = "Barrel";
                } else if (state.is(Blocks.SHULKER_BOX) || state.is(Blocks.WHITE_SHULKER_BOX) || state.is(Blocks.ORANGE_SHULKER_BOX)
                                || state.is(Blocks.MAGENTA_SHULKER_BOX) || state.is(Blocks.LIGHT_BLUE_SHULKER_BOX)
                                || state.is(Blocks.YELLOW_SHULKER_BOX) || state.is(Blocks.LIME_SHULKER_BOX)
                                || state.is(Blocks.PINK_SHULKER_BOX) || state.is(Blocks.GRAY_SHULKER_BOX)
                                || state.is(Blocks.LIGHT_GRAY_SHULKER_BOX) || state.is(Blocks.CYAN_SHULKER_BOX)
                                || state.is(Blocks.PURPLE_SHULKER_BOX) || state.is(Blocks.BLUE_SHULKER_BOX)
                                || state.is(Blocks.BROWN_SHULKER_BOX) || state.is(Blocks.GREEN_SHULKER_BOX)
                                || state.is(Blocks.RED_SHULKER_BOX) || state.is(Blocks.BLACK_SHULKER_BOX)) {
                        if (!shulkers.isOn()) return;
                        color = shulkerColor.get();
                        label = "Shulker";
                } else if (state.is(Blocks.HOPPER)) {
                        if (!hoppers.isOn()) return;
                        color = hopperColor.get();
                        label = "Hopper";
                } else if (state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)) {
                        if (!furnaces.isOn()) return;
                        color = furnaceColor.get();
                        label = "Furnace";
                } else if (state.is(Blocks.DROPPER) || state.is(Blocks.DISPENSER)) {
                        if (!droppers.isOn()) return;
                        color = dropperColor.get();
                        label = state.is(Blocks.DROPPER) ? "Dropper" : "Dispenser";
                } else {
                        return;
                }

                // Double chests render as one wide box
                AABB box = boxFor(state, pos);
                WorldShapes.filledBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                                Colors.withAlpha(color, fillOpacity.get().floatValue()), tw);
                WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, tw);
                if (showLabels.isOn()) {
                        WorldShapes.text(label, box.getCenter().x, box.maxY + 0.35, box.getCenter().z, 0.9f, Colors.WHITE, 0x90000000, tw);
                }
        }

        private AABB boxFor(BlockState state, BlockPos pos) {
                double x1 = pos.getX();
                double y1 = pos.getY();
                double z1 = pos.getZ();
                double x2 = x1 + 1;
                double y2 = y1 + 1;
                double z2 = z1 + 1;
                if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
                        ChestType type = state.getValue(ChestBlock.TYPE);
                        Direction facing = state.getValue(ChestBlock.FACING);
                        if (type == ChestType.LEFT || type == ChestType.RIGHT) {
                                Direction offsetDir = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
                                x1 += Math.min(0, offsetDir.getStepX());
                                x2 += Math.max(0, offsetDir.getStepX());
                                z1 += Math.min(0, offsetDir.getStepZ());
                                z2 += Math.max(0, offsetDir.getStepZ());
                        }
                }
                return new AABB(x1, y1, z1, x2, y2, z2);
        }
}
