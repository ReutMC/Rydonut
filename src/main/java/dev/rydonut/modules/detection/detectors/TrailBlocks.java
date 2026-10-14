package dev.rydonut.modules.detection.detectors;

import java.util.Set;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trail block classification: blocks players place along traveled paths
 * (torches, ice roads, obsidian highways, ladders, scaffolding).
 */
public final class TrailBlocks {
	private static final Set<Block> TRAIL = Set.of(
			Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH,
			Blocks.LANTERN, Blocks.SOUL_LANTERN,
			Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE,
			Blocks.OBSIDIAN, Blocks.LADDER, Blocks.SCAFFOLDING,
			Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.COBBLESTONE,
			Blocks.STONE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE,
			Blocks.RAIL, Blocks.POWERED_RAIL);

	private TrailBlocks() {
	}

	public static boolean isTrailBlock(BlockState state) {
		return TRAIL.contains(state.getBlock());
	}
}
