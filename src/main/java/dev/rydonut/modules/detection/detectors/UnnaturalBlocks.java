package dev.rydonut.modules.detection.detectors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Unnatural block" classification: blocks that do not generate naturally in
 * overworld/nether underground. Used by the UnnaturalBlockESP detector and fed
 * into the BaseFinder score. The list is user editable (block id strings) and
 * persisted in the config.
 */
public final class UnnaturalBlocks {
	private static volatile Set<Block> active = buildDefaultSet();

	private UnnaturalBlocks() {
	}

	public static List<String> defaultIds() {
		List<String> ids = new java.util.ArrayList<>();
		for (Block b : buildDefaultSet()) {
			ids.add(blockId(b));
		}
		java.util.Collections.sort(ids);
		return ids;
	}

	public static String blockId(Block block) {
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}

	/** Replaces the active set from user-provided block ids (invalid ids are ignored). */
	public static void applyList(List<String> ids) {
		Set<Block> set = new HashSet<>();
		for (String id : ids) {
			String normalized = id.contains(":") ? id : "minecraft:" + id;
			try {
				Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(normalized));
				if (block != Blocks.AIR) {
					set.add(block);
				}
			} catch (Exception ignored) {
			}
		}
		if (!set.isEmpty()) {
			active = set;
		}
	}

	public static boolean isUnnatural(BlockState state) {
		return active.contains(state.getBlock());
	}

	private static Set<Block> buildDefaultSet() {
		Set<Block> set = new HashSet<>();
		java.util.Collections.addAll(set,
				Blocks.COBBLESTONE,
				Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS,
				Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS,
				Blocks.GLASS, Blocks.TINTED_GLASS, Blocks.WHITE_WOOL, Blocks.BRICKS,
				Blocks.WHITE_CONCRETE, Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.TORCH, Blocks.WALL_TORCH,
				Blocks.OAK_SIGN, Blocks.OAK_WALL_SIGN, Blocks.WHITE_BED, Blocks.RED_BED, Blocks.BLUE_BED,
				Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL,
				Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.OBSERVER, Blocks.REDSTONE_BLOCK,
				Blocks.LEVER, Blocks.STONE_BUTTON, Blocks.OAK_BUTTON, Blocks.OAK_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE,
				Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR,
				Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.LADDER, Blocks.BOOKSHELF,
				Blocks.LOOM, Blocks.STONECUTTER, Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE,
				Blocks.SMITHING_TABLE, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.MOSSY_COBBLESTONE,
				Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM,
				Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS, Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB,
				Blocks.COBWEB, Blocks.JACK_O_LANTERN);
		return set;
	}
}
