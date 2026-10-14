package dev.rydonut.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.rydonut.config.ConfigManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.ChunkPos;

/**
 * Asynchronous, cached chunk scanner used by the whole detection suite.
 *
 * <p>Design rules:
 * <ul>
 *   <li>All scanning happens on a single daemon thread — the render thread
 *       only reads the concurrent result map, so scanning can never cause an
 *       FPS drop.</li>
 *   <li>Chunks are queued on load and re-queued when they become dirty; a
 *       time budget between chunk scans keeps the scanner thread cheap.</li>
 *   <li>Everything is read-only: no packets, no world interaction.</li>
 * </ul>
 */
public final class ChunkScanService {
        public static final ScanProfile DEFAULT_PROFILE = new ScanProfile(true, true, true, true, true, true, true);

        /** Options for one scan pass, rebuilt on the render thread when settings change. */
        public record ScanProfile(
                        boolean detectDeepslate,
                        boolean detectUnnatural,
                        boolean detectStorage,
                        boolean detectSpecial,
                        boolean detectTunnels,
                        boolean detectTrail,
                        boolean storePositions) {
        }

        /** User-configured block search set (BlockESP). Empty = disabled. */
        private static volatile java.util.Set<net.minecraft.world.level.block.Block> searchBlocks = java.util.Set.of();

        public static void setSearchBlocks(java.util.Set<net.minecraft.world.level.block.Block> blocks) {
                searchBlocks = blocks == null ? java.util.Set.of() : blocks;
        }

        public static boolean hasSearchBlocks() {
                return !searchBlocks.isEmpty();
        }

        private static final ExecutorService SCANNER = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Rydonut-Chunk-Scanner");
                t.setDaemon(true);
                return t;
        });

        private static final Map<Long, ChunkScanResult> CACHE = new ConcurrentHashMap<>();
        private static final ConcurrentLinkedQueue<Long> QUEUE = new ConcurrentLinkedQueue<>();
        private static final LongSet PENDING = new LongOpenHashSet(1024);
        private static final LongSet NEW_CHUNKS = new LongOpenHashSet(4096);

        // stats for /rydonut debug
        public static volatile long chunksScanned;
        public static volatile long scanHits;
        public static volatile long lastScanDurationMs;
        public static volatile int queueSize;

        private static volatile ScanProfile profile = DEFAULT_PROFILE;
        private static volatile int scanRangeChunks = 8;
        private static volatile int scanIntervalMs = 2000;
        private static volatile long lastTickNano = System.nanoTime();

        private ChunkScanService() {
        }

        public static void configure(ScanProfile scanProfile, int rangeChunks, int intervalMs) {
                profile = scanProfile == null ? DEFAULT_PROFILE : scanProfile;
                scanRangeChunks = Math.max(1, rangeChunks);
                scanIntervalMs = Math.max(250, intervalMs);
        }

        public static ChunkScanResult get(long chunkPos) {
                return CACHE.get(chunkPos);
        }

        public static boolean isNewChunk(long chunkPos) {
                return NEW_CHUNKS.contains(chunkPos);
        }

        public static void markNewChunk(long chunkPos) {
                NEW_CHUNKS.add(chunkPos);
        }

        public static void onChunkLoad(LevelChunk chunk) {
                long key = chunk.getPos().toLong();
                NEW_CHUNKS.add(key);
                CACHE.remove(key);
                enqueue(key);
        }

        public static void onChunkUnload(LevelChunk chunk) {
                long key = chunk.getPos().toLong();
                CACHE.remove(key);
                NEW_CHUNKS.remove(key);
        }

        /** Called every client tick: trims out-of-range cache entries and tops up the queue. */
        public static void tick(Minecraft mc) {
                ClientLevel level = mc.level;
                if (level == null) {
                        CACHE.clear();
                        QUEUE.clear();
                        synchronized (PENDING) {
                                PENDING.clear();
                        }
                        NEW_CHUNKS.clear();
                        return;
                }
                if (System.nanoTime() - lastTickNano < 250_000_000L) {
                        return; // re-plan at most 4x per second
                }
                lastTickNano = System.nanoTime();
                ChunkPos center = mc.player.chunkPosition();
                int range = scanRangeChunks;
                // drop far chunks
                CACHE.keySet().removeIf(key -> {
                        ChunkPos pos = new ChunkPos(key);
                        int d = Math.max(Math.abs(pos.x - center.x), Math.abs(pos.z - center.z));
                        return d > range + 4;
                });
                synchronized (PENDING) {
                        PENDING.removeIf(key -> {
                                ChunkPos pos = new ChunkPos(key);
                                int d = Math.max(Math.abs(pos.x - center.x), Math.abs(pos.z - center.z));
                                return d > range;
                        });
                }
                // enqueue chunks in range without a fresh result
                int enqueued = 0;
                for (int dx = -range; dx <= range && enqueued < 8; dx++) {
                        for (int dz = -range; dz <= range && enqueued < 8; dz++) {
                                long key = ChunkPos.asLong(center.x + dx, center.z + dz);
                                ChunkScanResult existing = CACHE.get(key);
                                boolean stale = existing == null || System.currentTimeMillis() - existing.lastScanMs > scanIntervalMs;
                                if (stale) {
                                        enqueue(key);
                                        enqueued++;
                                }
                        }
                }
                queueSize = QUEUE.size();
        }

        private static void enqueue(long key) {
                synchronized (PENDING) {
                        if (PENDING.add(key)) {
                                QUEUE.add(key);
                        }
                }
        }

        static {
                SCANNER.submit(() -> {
                        while (true) {
                                try {
                                        Long key = QUEUE.poll();
                                        if (key == null) {
                                                Thread.sleep(25);
                                                continue;
                                        }
                                        synchronized (PENDING) {
                                                PENDING.remove(key);
                                        }
                                        Minecraft mc = Minecraft.getInstance();
                                        ClientLevel level = mc.level;
                                        if (level == null || mc.player == null) {
                                                continue;
                                        }
                                        ChunkPos pos = new ChunkPos(key);
                                        LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
                                        if (chunk == null || chunk.isEmpty()) {
                                                continue;
                                        }
                                        long start = System.currentTimeMillis();
                                        ChunkScanResult result = CACHE.computeIfAbsent(key, ChunkScanResult::new);
                                        scanChunk(level, chunk, result, profile);
                                        result.lastScanMs = System.currentTimeMillis();
                                        lastScanDurationMs = System.currentTimeMillis() - start;
                                        chunksScanned++;
                                        if (!result.anyPositionStored() && result.tunnels.isEmpty()) {
                                                scanHits += 0;
                                        } else {
                                                scanHits++;
                                        }
                                        // simple time budget so the scanner thread stays light
                                        Thread.sleep(2);
                                } catch (InterruptedException e) {
                                        return;
                                } catch (Exception e) {
                                        ConfigManager.RydonutLogger.warn("Scan error: " + e);
                                }
                        }
                });
        }

        // ---------------------------------------------------------------- scanning

        private static void scanChunk(ClientLevel level, LevelChunk chunk, ChunkScanResult out, ScanProfile p) {
                out.reset();
                out.newChunk = NEW_CHUNKS.contains(out.chunkPos);
                ChunkPos cpos = chunk.getPos();
                int baseX = cpos.getMinBlockX();
                int baseZ = cpos.getMinBlockZ();
                boolean store = p.storePositions();

                // block entities first (cheap: map iteration)
                if (p.detectStorage() || p.detectSpecial()) {
                        for (Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                                BlockPos bep = entry.getKey();
                                net.minecraft.world.level.block.entity.BlockEntity be = entry.getValue();
                                if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.HopperBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.DispenserBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity) {
                                        out.add(ChunkScanResult.Cat.STORAGE_BE, bep, store);
                                } else if (be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity) {
                                        out.add(ChunkScanResult.Cat.SPAWNER, bep, store);
                                } else if (be instanceof net.minecraft.world.level.block.entity.EnchantingTableBlockEntity) {
                                        out.add(ChunkScanResult.Cat.ENCHANT_TABLE, bep, store);
                                } else if (be instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity) {
                                        out.add(ChunkScanResult.Cat.BEACON, bep, store);
                                } else if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity
                                                || be instanceof net.minecraft.world.level.block.entity.BannerBlockEntity) {
                                        out.add(ChunkScanResult.Cat.SIGN_OR_BANNER, bep, store);
                                }
                        }
                }

                // block state scan, section by section
                LevelChunkSection[] sections = chunk.getSections();
                int minSectionY = level.getMinSectionY();
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                int maxY = level.getMaxY();
                int minY = level.getMinY();

                for (int si = 0; si < sections.length; si++) {
                        LevelChunkSection section = sections[si];
                        if (section == null || section.hasOnlyAir()) {
                                continue;
                        }
                        int sy = (minSectionY + si) << 4;
                        for (int y = 0; y < 16; y++) {
                                int wy = sy + y;
                                if (wy < minY || wy >= maxY) {
                                        continue;
                                }
                                boolean deepslateBand = wy <= 40 && wy >= minY; // natural deepslate band + transition
                                for (int z = 0; z < 16; z++) {
                                        for (int x = 0; x < 16; x++) {
                                                BlockState state = section.getBlockState(x, y, z);
                                                if (state.isAir()) {
                                                        continue;
                                                }
                                                cursor.set(baseX + x, wy, baseZ + z);
                                                classify(level, chunk, out, p, state, cursor, store, deepslateBand);
                                        }
                                }
                        }
                }

                if (p.detectTunnels()) {
                        detectTunnels(level, out, baseX, baseZ, minY, maxY);
                }

                // user block search
                if (hasSearchBlocks()) {
                        java.util.Set<net.minecraft.world.level.block.Block> wanted = searchBlocks;
                        BlockPos.MutableBlockPos searchCursor = new BlockPos.MutableBlockPos();
                        LevelChunkSection[] allSections = chunk.getSections();
                        int minSy = level.getMinSectionY();
                        for (int si = 0; si < allSections.length; si++) {
                                LevelChunkSection section = allSections[si];
                                if (section == null || section.hasOnlyAir()) {
                                        continue;
                                }
                                int sy = (minSy + si) << 4;
                                for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                                for (int x = 0; x < 16; x++) {
                                                        BlockState st = section.getBlockState(x, y, z);
                                                        if (wanted.contains(st.getBlock())) {
                                                                searchCursor.set(baseX + x, sy + y, baseZ + z);
                                                                out.add(ChunkScanResult.Cat.SEARCH, searchCursor, true);
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        private static void classify(ClientLevel level, LevelChunk chunk, ChunkScanResult out, ScanProfile p,
                        BlockState state, BlockPos.MutableBlockPos pos, boolean store, boolean deepslateBand) {
                BlockState s = state;
                net.minecraft.world.level.block.Block block = s.getBlock();

                if (p.detectDeepslate() && deepslateBand) {
                        // Rotated deepslate: natural generation always has axis == Y.
                        if (block instanceof RotatedPillarBlock && s.hasProperty(RotatedPillarBlock.AXIS)) {
                                Direction.Axis axis = s.getValue(RotatedPillarBlock.AXIS);
                                boolean isDeepslate = s.is(Blocks.DEEPSLATE)
                                                || s.is(Blocks.COBBLED_DEEPSLATE)
                                                || s.is(Blocks.POLISHED_DEEPSLATE)
                                                || s.is(Blocks.DEEPSLATE_BRICKS)
                                                || s.is(Blocks.DEEPSLATE_TILES)
                                                || s.is(Blocks.CHISELED_DEEPSLATE)
                                                || s.is(Blocks.CRACKED_DEEPSLATE_BRICKS)
                                                || s.is(Blocks.CRACKED_DEEPSLATE_TILES);
                                if (isDeepslate && axis != Direction.Axis.Y) {
                                        out.add(ChunkScanResult.Cat.ROTATED_DEEPSLATE, pos, store);
                                }
                        }
                }

                if (p.detectUnnatural()) {
                        if (dev.rydonut.modules.detection.detectors.UnnaturalBlocks.isUnnatural(s)) {
                                out.add(ChunkScanResult.Cat.UNNATURAL, pos, store);
                        }
                }

                if (p.detectTrail()) {
                        if (dev.rydonut.modules.detection.detectors.TrailBlocks.isTrailBlock(s)) {
                                out.add(ChunkScanResult.Cat.TRAIL, pos, store);
                        }
                }

                if (s.is(Blocks.ANCIENT_DEBRIS)) {
                        out.add(ChunkScanResult.Cat.DEBRIS, pos, store);
                }

                if (p.detectSpecial()) {
                        if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)
                                        || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.GLOWSTONE)
                                        || s.is(Blocks.SEA_LANTERN) || s.is(Blocks.JACK_O_LANTERN) || s.is(Blocks.SHROOMLIGHT)
                                        || s.is(Blocks.CAMPFIRE) || s.is(Blocks.REDSTONE_TORCH)) {
                                out.add(ChunkScanResult.Cat.LIGHT_SOURCE, pos, false);
                        }
                        if (s.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                                out.add(ChunkScanResult.Cat.BED, pos, store);
                        }
                        if (s.is(Blocks.RESPAWN_ANCHOR)) {
                                out.add(ChunkScanResult.Cat.RESPAWN_ANCHOR, pos, store);
                        }
                        if (s.is(Blocks.END_PORTAL_FRAME)) {
                                out.add(ChunkScanResult.Cat.END_PORTAL_FRAME, pos, store);
                        }
                        if (s.is(Blocks.ANVIL) || s.is(Blocks.CHIPPED_ANVIL) || s.is(Blocks.DAMAGED_ANVIL)) {
                                out.add(ChunkScanResult.Cat.ANVIL, pos, store);
                        }
                        if (s.is(Blocks.NETHER_PORTAL)) {
                                out.add(ChunkScanResult.Cat.PORTAL_OBSIDIAN, pos, store);
                        }
                        if (s.is(Blocks.OBSIDIAN)) {
                                // Count obsidian for portal frame heuristic (a frame has >= 10).
                                out.counts[ChunkScanResult.Cat.PORTAL_OBSIDIAN.ordinal()]++;
                        }
                        if (s.is(Blocks.FARMLAND) || s.is(Blocks.WHEAT) || s.is(Blocks.CARROTS) || s.is(Blocks.POTATOES)
                                        || s.is(Blocks.BEETROOTS) || s.is(Blocks.SUGAR_CANE) || s.is(Blocks.KELP_PLANT)
                                        || s.is(Blocks.MELON) || s.is(Blocks.PUMPKIN)) {
                                out.add(ChunkScanResult.Cat.FARM, pos, false);
                        }
                }
        }

        /**
         * Tunnel detector: flags long straight air corridors with solid walls
         * (classic 1x2 tunnels / branch mines). Samples a 3D grid, so cost stays low.
         */
        private static void detectTunnels(ClientLevel level, ChunkScanResult out, int baseX, int baseZ, int minY, int maxY) {
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                // Horizontal runs along X and Z at sampled Y levels
                for (int wy = Math.max(minY + 4, -58); wy <= Math.min(120, maxY); wy += 4) {
                        for (int z = 0; z < 16; z++) {
                                int runStart = -1;
                                for (int x = 0; x <= 16; x++) {
                                        boolean air = x < 16 && isAirColumn(level, cursor, baseX + x, wy, baseZ + z);
                                        if (air && runStart < 0) {
                                                runStart = x;
                                        } else if (!air && runStart >= 0) {
                                                int len = x - runStart;
                                                if (len >= 10) {
                                                        out.tunnels.add(new double[]{
                                                                        baseX + runStart, wy, baseZ + z,
                                                                        baseX + x - 1, wy, baseZ + z});
                                                }
                                                runStart = -1;
                                        }
                                }
                        }
                        for (int x = 0; x < 16; x++) {
                                int runStart = -1;
                                for (int z = 0; z <= 16; z++) {
                                        boolean air = z < 16 && isAirColumn(level, cursor, baseX + x, wy, baseZ + z);
                                        if (air && runStart < 0) {
                                                runStart = z;
                                        } else if (!air && runStart >= 0) {
                                                int len = z - runStart;
                                                if (len >= 10) {
                                                        out.tunnels.add(new double[]{
                                                                        baseX + x, wy, baseZ + runStart,
                                                                        baseX + x, wy, baseZ + z - 1});
                                                }
                                                runStart = -1;
                                        }
                                }
                        }
                }
        }

        private static boolean isAirColumn(ClientLevel level, BlockPos.MutableBlockPos cursor, int x, int y, int z) {
                // 1x2 tunnel profile: head-level and feet-level air, solid below
                cursor.set(x, y, z);
                boolean head = level.getBlockState(cursor).isAir();
                cursor.set(x, y - 1, z);
                boolean feet = level.getBlockState(cursor).isAir();
                cursor.set(x, y - 2, z);
                boolean floor = !level.getBlockState(cursor).isAir();
                return head && feet && floor;
        }

        // ---------------------------------------------------------------- stats

        public static String debugStats() {
                return "chunksScanned=" + chunksScanned + ", queue=" + queueSize
                                + ", cached=" + CACHE.size() + ", lastScanMs=" + lastScanDurationMs
                                + ", hits=" + scanHits;
        }

        public static int cachedCount() {
                return CACHE.size();
        }

        /** Top suspicious chunks as (chunkPos, score) pairs, computed by BaseFinder. */
        public static Iterator<Map.Entry<Long, ChunkScanResult>> cacheIterator() {
                return CACHE.entrySet().iterator();
        }

}
