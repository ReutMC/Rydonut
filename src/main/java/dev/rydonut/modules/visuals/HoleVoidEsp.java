package dev.rydonut.modules.visuals;

import java.util.ArrayList;
import java.util.List;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Hole ESP (safe 1x1/1x2 holes near the player) and Void ESP (bedrock holes
 * near the world bottom / nether roof). Scanned on the tick thread in a small
 * volume, results cached between renders.
 */
public class HoleVoidEsp extends Module {
	private final BoolSetting holes = new BoolSetting("Hole ESP", true);
	private final BoolSetting voidHoles = new BoolSetting("Void ESP", true);
	private final DoubleSetting holeRange = new DoubleSetting("Hole range", 8, 4, 16, 1);
	private final BoolSetting netherRoof = new BoolSetting("Scan nether roof", true);

	private final List<double[]> foundHoles = new ArrayList<>();
	private final List<double[]> foundVoid = new ArrayList<>();
	private int scanCooldown;
	private BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

	public HoleVoidEsp() {
		super("Hole & Void ESP", Category.VISUALS, "Highlights safe holes and void edges for navigation.");
		addSettings(holes, voidHoles, holeRange, netherRoof);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.level == null) {
			return;
		}
		if (--scanCooldown > 0) {
			return;
		}
		scanCooldown = 4;
		foundHoles.clear();
		foundVoid.clear();
		int px = mc.player.blockPosition().getX();
		int py = mc.player.blockPosition().getY();
		int pz = mc.player.blockPosition().getZ();
		int r = holeRange.get().intValue();

		if (holes.isOn()) {
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					if (Math.abs(x) + Math.abs(z) > r + 1) {
						continue;
					}
					int bx = px + x;
					int bz = pz + z;
					// scan a few Y levels around the player
					for (int dy = -2; dy <= 2; dy++) {
						int by = py + dy;
						if (isHole(bx, by, bz)) {
							foundHoles.add(new double[]{bx, by, bz});
						}
					}
				}
			}
		}
		if (voidHoles.isOn()) {
			ChunkPos center = mc.player.chunkPosition();
			int minY = mc.level.getMinY();
			boolean nether = mc.level.dimensionType().hasCeiling();
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					LevelChunk chunk = mc.level.getChunkSource().getChunk(center.x + dx, center.z + dz, ChunkStatus.FULL, false);
					if (chunk == null) {
						continue;
					}
					int baseX = chunk.getPos().getMinBlockX();
					int baseZ = chunk.getPos().getMinBlockZ();
					// bedrock floor holes
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							for (int y = minY; y <= minY + 2; y++) {
								cursor.set(baseX + x, y, baseZ + z);
								if (!mc.level.getBlockState(cursor).is(Blocks.BEDROCK)) {
									foundVoid.add(new double[]{baseX + x, y, baseZ + z});
								}
							}
							if (nether && netherRoof.isOn()) {
								int top = mc.level.getMaxY() - 1;
								cursor.set(baseX + x, top, baseZ + z);
								if (!mc.level.getBlockState(cursor).is(Blocks.BEDROCK)) {
									foundVoid.add(new double[]{baseX + x, top, baseZ + z});
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean isHole(int x, int y, int z) {
		cursor.set(x, y, z);
		if (!mc.level.getBlockState(cursor).isAir()) {
			return false;
		}
		cursor.set(x, y - 1, z);
		boolean floorSolid = !mc.level.getBlockState(cursor).isAir() && !mc.level.getBlockState(cursor).liquid();
		cursor.set(x, y - 2, z);
		boolean floorDeepSolid = !mc.level.getBlockState(cursor).isAir();
		if (!(floorSolid && floorDeepSolid)) {
			return false;
		}
		// at least 2 solid walls (classic crystal-pvp safe hole)
		int walls = 0;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] d : dirs) {
			cursor.set(x + d[0], y, z + d[1]);
			if (!mc.level.getBlockState(cursor).isAir()) {
				walls++;
			}
		}
		return walls >= 2;
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			if (holes.isOn()) {
				for (double[] hole : foundHoles) {
					double x = hole[0], y = hole[1], z = hole[2];
					WorldShapes.filledBox(x, y, z, x + 1, y + 1, z + 1, Colors.withAlpha(Colors.SUCCESS, 0.25f), true);
					WorldShapes.outlinedBox(x, y, z, x + 1, y + 1, z + 1, Colors.SUCCESS, true);
				}
			}
			if (voidHoles.isOn()) {
				for (double[] hole : foundVoid) {
					double x = hole[0], y = hole[1], z = hole[2];
					WorldShapes.filledBox(x, y, z, x + 1, y + 1, z + 1, Colors.withAlpha(Colors.DANGER, 0.4f), true);
					WorldShapes.outlinedBox(x, y, z, x + 1, y + 1, z + 1, Colors.DANGER, true);
				}
			}
		} finally {
			WorldShapes.end();
		}
	}

}
