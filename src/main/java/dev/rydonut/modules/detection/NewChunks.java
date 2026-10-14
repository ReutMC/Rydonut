package dev.rydonut.modules.detection;

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
 * NewChunks detector (heuristic, read-only): marks chunks that arrived from
 * the server during this session and had active flowing liquids or player
 * trail blocks right at load time — the classic client-side "new chunk"
 * heuristic. Renders a tinted floor overlay; never sends anything.
 */
public class NewChunks extends Module {
	private final BoolSetting overlay = new BoolSetting("Floor overlay", true);
	private final BoolSetting rememberSession = new BoolSetting("Session memory", true);
	private final ColorSetting newColor = new ColorSetting("New chunk color", 0xFF5DFF7A);
	private final ColorSetting oldColor = new ColorSetting("Old chunk color", 0x50FF5D7A);

	public NewChunks() {
		super("NewChunks", Category.DETECTION, "Heuristic new-vs-old chunk overlay (client-side only).");
		addSettings(overlay, rememberSession, newColor, oldColor);
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (!overlay.isOn() || mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			ChunkPos center = mc.player.chunkPosition();
			int r = 6;
			double y = mc.level.getMinY() + 0.05;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					long key = ChunkPos.asLong(center.x + dx, center.z + dz);
					boolean isNew = ChunkScanService.isNewChunk(key);
					if (!rememberSession.isOn() && !isNew) {
						continue;
					}
					int color = isNew ? newColor.get() : oldColor.get();
					double x1 = (center.x + dx) * 16;
					double z1 = (center.z + dz) * 16;
					WorldShapes.filledBox(x1, y, z1, x1 + 16, y + 0.02, z1 + 16, Colors.withAlpha(color, 0.35f), true);
				}
			}
		} finally {
			WorldShapes.end();
		}
	}
}
