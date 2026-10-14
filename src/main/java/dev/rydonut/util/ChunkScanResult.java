package dev.rydonut.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Per-chunk scan result. Filled by the background scanner thread, read by the
 * render thread. Counts are plain ints; flagged positions are stored per
 * category only when a rendering module asks for them (keeps memory bounded).
 */
public class ChunkScanResult {
        /** Renderable/trackable categories collected by the scanner. */
        public enum Cat {
                ROTATED_DEEPSLATE,
                UNNATURAL,
                STORAGE_BE,
                SPAWNER,
                ENCHANT_TABLE,
                ANVIL,
                BEACON,
                BED,
                RESPAWN_ANCHOR,
                END_PORTAL_FRAME,
                LIGHT_SOURCE,
                FARM,
                SIGN_OR_BANNER,
                PORTAL_OBSIDIAN,
                TRAIL,
                DEBRIS,
                TUNNEL_AIR,
                SEARCH
        }

        public static final Cat[] RENDER_CATS = {
                        Cat.ROTATED_DEEPSLATE, Cat.UNNATURAL, Cat.STORAGE_BE, Cat.SPAWNER,
                        Cat.ENCHANT_TABLE, Cat.ANVIL, Cat.BEACON, Cat.BED, Cat.RESPAWN_ANCHOR,
                        Cat.END_PORTAL_FRAME, Cat.LIGHT_SOURCE, Cat.FARM, Cat.SIGN_OR_BANNER,
                        Cat.PORTAL_OBSIDIAN, Cat.TRAIL, Cat.DEBRIS, Cat.SEARCH
        };

        public final long chunkPos;
        public long lastScanMs;
        public boolean newChunk;

        /** Counts per category (indexed by Cat ordinal). */
        public final int[] counts = new int[Cat.values().length];
        /** Flagged positions per category; only filled for categories requested by the scan profile. */
        @SuppressWarnings("unchecked")
        public final List<BlockPos>[] positions = new List[Cat.values().length];

        /** Player-made tunnel segments found in this chunk (start..end pairs flattened). */
        public final List<double[]> tunnels = new ArrayList<>();

        public ChunkScanResult(long chunkPos) {
                this.chunkPos = chunkPos;
        }

        public int count(Cat cat) {
                return counts[cat.ordinal()];
        }

        public List<BlockPos> positions(Cat cat) {
                List<BlockPos> list = positions[cat.ordinal()];
                return list == null ? List.of() : list;
        }

        public void add(Cat cat, BlockPos pos, boolean storePosition) {
                counts[cat.ordinal()]++;
                if (storePosition) {
                        List<BlockPos> list = positions[cat.ordinal()];
                        if (list == null) {
                                list = new ArrayList<>();
                                positions[cat.ordinal()] = list;
                        }
                        if (list.size() < 4096) {
                                list.add(pos.immutable());
                        }
                }
        }

        public void reset() {
                java.util.Arrays.fill(counts, 0);
                for (int i = 0; i < positions.length; i++) {
                        if (positions[i] != null) {
                                positions[i].clear();
                        }
                }
                tunnels.clear();
        }

        public boolean anyPositionStored() {
                for (List<BlockPos> l : positions) {
                        if (l != null && !l.isEmpty()) {
                                return true;
                        }
                }
                return false;
        }

        public boolean is(BlockState state, Block block) {
                return state.is(block);
        }
}
