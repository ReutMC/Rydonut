package dev.rydonut.modules.visuals;

import java.util.HashSet;
import java.util.Set;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.IntSetting;
import dev.rydonut.settings.StringListSetting;
import dev.rydonut.util.ChunkScanResult;
import dev.rydonut.util.ChunkScanService;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/**
 * Block ESP / search: highlights a user-defined block list (ores, spawners...)
 * using the async chunk scanner, so rendering never blocks.
 */
public class BlockEsp extends Module {
	private final DoubleSetting range = new DoubleSetting("Range (chunks)", 6, 1, 16, 1);
	private final IntSetting updateInterval = new IntSetting("Scan interval (ms)", 1500, 250, 10000, 250);
	private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
	private final BoolSetting showLabels = new BoolSetting("Count labels", true);
	private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.3, 0.05, 1.0, 0.05);
	private final StringListSetting blockList = new StringListSetting("Blocks",
			java.util.List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
					"minecraft:ancient_debris", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
					"minecraft:spawner", "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore"));

	public BlockEsp() {
		super("Block ESP", Category.VISUALS, "Searches a configurable block list in loaded chunks.");
		addSettings(range, updateInterval, throughWalls, showLabels, fillOpacity, blockList);
		blockList.onChange(this::applyList);
	}

	@Override
	protected void onEnable() {
		applyList(blockList.get());
	}

	private void applyList(java.util.List<String> ids) {
		Set<Block> blocks = new HashSet<>();
		for (String id : ids) {
			String normalized = id.contains(":") ? id : "minecraft:" + id;
			try {
				Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(normalized));
				if (block != Blocks.AIR) {
					blocks.add(block);
				}
			} catch (Exception ignored) {
			}
		}
		ChunkScanService.setSearchBlocks(blocks);
		ChunkScanService.configure(ChunkScanService.DEFAULT_PROFILE, range.get().intValue(), updateInterval.get());
	}

	@Override
	public void onTick() {
		ChunkScanService.configure(ChunkScanService.DEFAULT_PROFILE, range.get().intValue(), updateInterval.get());
		ChunkScanService.tick(mc);
	}

	@Override
	public void onDisable() {
		ChunkScanService.setSearchBlocks(java.util.Set.of());
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || !ChunkScanService.hasSearchBlocks()) {
			return;
		}
		WorldShapes.begin(context);
		try {
			ChunkPos center = mc.player.chunkPosition();
			int range = this.range.get().intValue();
			int found = 0;
			for (int dx = -range; dx <= range; dx++) {
				for (int dz = -range; dz <= range; dz++) {
					ChunkScanResult result = ChunkScanService.get(ChunkPos.asLong(center.x + dx, center.z + dz));
					if (result == null) {
						continue;
					}
					for (BlockPos pos : result.positions(ChunkScanResult.Cat.SEARCH)) {
						found++;
						boolean tw = throughWalls.isOn();
						WorldShapes.filledBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
								Colors.withAlpha(Colors.ACCENT, fillOpacity.get().floatValue()), tw);
						WorldShapes.outlinedBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
								Colors.PRIMARY, tw);
					}
				}
			}
			if (showLabels.isOn() && found > 0) {
				WorldShapes.text("Found " + found, mc.player.getX(), mc.player.getEyeY() + 1.0, mc.player.getZ(), 1.0f,
						Colors.WHITE, 0x90000000, true);
			}
		} finally {
			WorldShapes.end();
		}
	}

	@SuppressWarnings("unused")
	private static AABB unused() {
		return new AABB(0, 0, 0, 1, 1, 1);
	}
}
