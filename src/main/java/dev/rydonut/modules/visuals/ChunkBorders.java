package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.util.ChunkScanService;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.level.ChunkPos;

/**
 * Chunk border viewer with configurable colors, plus optional new/old chunk
 * tinting from the NewChunks detector.
 */
public class ChunkBorders extends Module {
	private final DoubleSetting height = new DoubleSetting("Height", 32, 8, 128, 8);
	private final BoolSetting showCurrentOnly = new BoolSetting("Current chunk only", false);
	private final BoolSetting tintNewChunks = new BoolSetting("Tint new chunks", true);
	private final ColorSetting borderColor = new ColorSetting("Border color", 0xFF4FC3F7);

	public ChunkBorders() {
		super("Chunk Borders", Category.VISUALS, "Shows chunk borders (F3+G style) with new-chunk tinting.");
		addSettings(height, showCurrentOnly, tintNewChunks, borderColor);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			ChunkPos center = mc.player.chunkPosition();
			double y = mc.player.getY();
			double yMin = mc.level.getMinY();
			double yMax = Math.min(mc.level.getMaxY(), y + height.get());
			if (showCurrentOnly.isOn()) {
				WorldShapes.chunkBorder(center.x, center.z, yMin, yMax, borderColor.get());
			} else {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						int cx = center.x + dx;
						int cz = center.z + dz;
						int color = borderColor.get();
						if (tintNewChunks.isOn() && ChunkScanService.isNewChunk(ChunkPos.asLong(cx, cz))) {
							color = Colors.lerp(color, Colors.SUCCESS, 0.65f);
						}
						WorldShapes.chunkBorder(cx, cz, yMin, yMax, color);
					}
				}
			}
		} finally {
			WorldShapes.end();
		}
	}
}
