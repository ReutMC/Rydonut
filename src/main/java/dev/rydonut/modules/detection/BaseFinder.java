package dev.rydonut.modules.detection;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.modules.visuals.Waypoints;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.IntSetting;
import dev.rydonut.util.ChunkScanResult;
import dev.rydonut.util.ChunkScanService;
import dev.rydonut.util.Notifications;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.level.ChunkPos;

/**
 * BaseFinder (master module): combines the sub-detectors of the async chunk
 * scanner into a per-chunk "suspicion score". Chunks above the threshold are
 * highlighted with a chunk-border box and a floating label. Includes a top-N
 * suspicious chunk list (clickable in the GUI to create waypoints), HUD/chat
 * notifications with cooldown and timestamped find exports.
 *
 * <p>Completely passive: it only reads data the client already has.
 */
public class BaseFinder extends Module {
	// --- detector weights (all individually toggleable) ---
	private final BoolSetting deepslateDetector = new BoolSetting("Rotated deepslate", true);
	private final DoubleSetting deepslateWeight = new DoubleSetting("DS weight", 3.0, 0.0, 20.0, 0.5);
	private final BoolSetting unnaturalDetector = new BoolSetting("Unnatural blocks", true);
	private final DoubleSetting unnaturalWeight = new DoubleSetting("UB weight", 1.0, 0.0, 20.0, 0.5);
	private final BoolSetting storageDetector = new BoolSetting("Storage clusters", true);
	private final DoubleSetting storageWeight = new DoubleSetting("Storage weight", 0.4, 0.0, 20.0, 0.1);
	private final BoolSetting spawnerDetector = new BoolSetting("Spawners", true);
	private final DoubleSetting spawnerWeight = new DoubleSetting("Spawner weight", 1.5, 0.0, 20.0, 0.5);
	private final BoolSetting specialDetector = new BoolSetting("Special blocks", true);
	private final DoubleSetting specialWeight = new DoubleSetting("Special weight", 1.0, 0.0, 20.0, 0.5);
	private final BoolSetting tunnelDetector = new BoolSetting("Tunnels", true);
	private final DoubleSetting tunnelWeight = new DoubleSetting("Tunnel weight", 2.0, 0.0, 20.0, 0.5);
	private final BoolSetting lightDetector = new BoolSetting("Light sources", true);
	private final DoubleSetting lightWeight = new DoubleSetting("Light weight", 0.2, 0.0, 20.0, 0.1);
	private final BoolSetting farmDetector = new BoolSetting("Farms", true);
	private final DoubleSetting farmWeight = new DoubleSetting("Farm weight", 0.3, 0.0, 20.0, 0.1);
	private final BoolSetting trailDetector = new BoolSetting("Trail blocks", true);
	private final DoubleSetting trailWeight = new DoubleSetting("Trail weight", 0.5, 0.0, 20.0, 0.1);
	private final BoolSetting portalDetector = new BoolSetting("Portals", true);
	private final DoubleSetting portalWeight = new DoubleSetting("Portal weight", 1.0, 0.0, 20.0, 0.5);
	private final BoolSetting debrisDetector = new BoolSetting("Ancient debris", true);
	private final DoubleSetting debrisWeight = new DoubleSetting("Debris weight", 4.0, 0.0, 20.0, 0.5);

	// --- scoring / rendering ---
	private final DoubleSetting threshold = new DoubleSetting("Score threshold", 8.0, 1.0, 100.0, 1.0);
	private final DoubleSetting range = new DoubleSetting("Range (chunks)", 8, 1, 16, 1);
	private final IntSetting updateInterval = new IntSetting("Scan interval (ms)", 2500, 250, 10000, 250);
	private final BoolSetting showLabels = new BoolSetting("Labels", true);
	private final BoolSetting notifications = new BoolSetting("Notifications", true);
	private final BoolSetting exportFinds = new BoolSetting("Export finds", true);
	private final IntSetting topCount = new IntSetting("Top N list", 5, 1, 15, 1);
	private final ColorSetting color = new ColorSetting("Color", 0xFFFF9A3D);

	private final List<TopChunk> topChunks = new ArrayList<>();
	private final List<Long> notifiedChunks = new ArrayList<>();

	public BaseFinder() {
		super("BaseFinder", Category.DETECTION, "Finds possible player bases from chunk scan data.");
		addSettings(deepslateDetector, deepslateWeight, unnaturalDetector, unnaturalWeight,
				storageDetector, storageWeight, spawnerDetector, spawnerWeight,
				specialDetector, specialWeight, tunnelDetector, tunnelWeight,
				lightDetector, lightWeight, farmDetector, farmWeight,
				trailDetector, trailWeight, portalDetector, portalWeight,
				debrisDetector, debrisWeight,
				threshold, range, updateInterval, showLabels, notifications, exportFinds, topCount, color);
	}

	@Override
	protected void onEnable() {
		dev.rydonut.modules.detection.detectors.UnnaturalBlocks.applyList(
				dev.rydonut.modules.detection.detectors.UnnaturalBlocks.defaultIds());
	}

	@Override
	public void onTick() {
		ChunkScanService.configure(new ChunkScanService.ScanProfile(
				deepslateDetector.isOn(), unnaturalDetector.isOn(), storageDetector.isOn(),
				specialDetector.isOn(), tunnelDetector.isOn(), trailDetector.isOn(), true),
				range.get().intValue(), updateInterval.get());
		ChunkScanService.tick(mc);
		recalculateTop();
	}

	private double score(ChunkScanResult result) {
		double score = 0;
		if (deepslateDetector.isOn()) {
			score += deepslateWeight.get() * Math.min(result.count(ChunkScanResult.Cat.ROTATED_DEEPSLATE), 40);
		}
		if (unnaturalDetector.isOn()) {
			score += unnaturalWeight.get() * Math.min(result.count(ChunkScanResult.Cat.UNNATURAL), 60);
		}
		if (storageDetector.isOn()) {
			score += storageWeight.get() * Math.min(result.count(ChunkScanResult.Cat.STORAGE_BE), 40);
		}
		if (spawnerDetector.isOn()) {
			score += spawnerWeight.get() * Math.min(result.count(ChunkScanResult.Cat.SPAWNER), 8);
		}
		if (specialDetector.isOn()) {
			score += specialWeight.get() * Math.min(
					result.count(ChunkScanResult.Cat.ENCHANT_TABLE) + result.count(ChunkScanResult.Cat.ANVIL)
							+ result.count(ChunkScanResult.Cat.BEACON) + result.count(ChunkScanResult.Cat.BED)
							+ result.count(ChunkScanResult.Cat.RESPAWN_ANCHOR) + result.count(ChunkScanResult.Cat.END_PORTAL_FRAME),
					30);
		}
		if (tunnelDetector.isOn()) {
			score += tunnelWeight.get() * Math.min(result.tunnels.size(), 10);
		}
		if (lightDetector.isOn()) {
			score += lightWeight.get() * Math.min(result.count(ChunkScanResult.Cat.LIGHT_SOURCE), 80);
		}
		if (farmDetector.isOn()) {
			score += farmWeight.get() * Math.min(result.count(ChunkScanResult.Cat.FARM), 100);
		}
		if (trailDetector.isOn()) {
			score += trailWeight.get() * Math.min(result.count(ChunkScanResult.Cat.TRAIL), 50);
		}
		if (portalDetector.isOn()) {
			score += portalWeight.get() * Math.min(result.count(ChunkScanResult.Cat.PORTAL_OBSIDIAN) / 10, 4);
		}
		if (debrisDetector.isOn()) {
			score += debrisWeight.get() * Math.min(result.count(ChunkScanResult.Cat.DEBRIS), 12);
		}
		return score;
	}

	private void recalculateTop() {
		if (mc.player == null) {
			return;
		}
		topChunks.clear();
		ChunkPos center = mc.player.chunkPosition();
		var it = ChunkScanService.cacheIterator();
		double thresholdValue = threshold.get();
		List<TopChunk> found = new ArrayList<>();
		while (it.hasNext()) {
			var entry = it.next();
			ChunkScanResult result = entry.getValue();
			ChunkPos pos = new ChunkPos(entry.getKey());
			int dist = Math.max(Math.abs(pos.x - center.x), Math.abs(pos.z - center.z));
			if (dist > range.get() + 2) {
				continue;
			}
			double s = score(result);
			if (s >= thresholdValue) {
				found.add(new TopChunk(pos.x, pos.z, s, dist));
				notifyIfNeeded(entry.getKey(), pos, s);
			}
		}
		found.sort((a, b) -> Double.compare(b.score, a.score));
		for (int i = 0; i < found.size() && i < topCount.get(); i++) {
			topChunks.add(found.get(i));
		}
	}

	private void notifyIfNeeded(long key, ChunkPos pos, double score) {
		if (!notifications.isOn() || notifiedChunks.contains(key)) {
			return;
		}
		notifiedChunks.add(key);
		if (notifiedChunks.size() > 512) {
			notifiedChunks.remove(0);
		}
		Notifications.push("Possible base", "Chunk " + pos.x + ", " + pos.z + " (score " + (int) score + ")");
		if (exportFinds.isOn()) {
			String dimension = mc.level == null ? "?" : mc.level.dimension().identifier().toString();
			ConfigManager.exportFind(dimension, "Possible base chunk " + pos.x + ", " + pos.z + " score=" + (int) score);
		}
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			double yMin = mc.level.getMinY();
			double yMax = Math.min(mc.level.getMaxY(), mc.player.getY() + 24);
			for (TopChunk top : topChunks) {
				int c = color.get();
				WorldShapes.chunkBorder(top.chunkX, top.chunkZ, yMin, yMax, c);
				if (showLabels.isOn()) {
					double lx = top.chunkX * 16 + 8;
					double lz = top.chunkZ * 16 + 8;
					WorldShapes.text("Possible base (score " + (int) top.score + ")", lx, yMax + 1.0, lz, 1.2f,
							Colors.WHITE, 0xA0000000, true);
				}
			}
		} finally {
			WorldShapes.end();
		}
	}

	/** Snapshot of the top suspicious chunks for the GUI list. */
	public List<TopChunk> getTopChunks() {
		return topChunks;
	}

	public void clearNotified() {
		notifiedChunks.clear();
	}

	public record TopChunk(int chunkX, int chunkZ, double score, int distance) {
		public void createWaypoint() {
			Waypoints.addChunk(ChunkPos.asLong(chunkX, chunkZ), "Base " + chunkX + "," + chunkZ, Colors.DANGER);
		}
	}
}
