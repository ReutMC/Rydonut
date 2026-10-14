package dev.rydonut.modules.detection;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.StringSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

/**
 * Biome & slime chunk overlay (HUD). Also includes a bedrock-layer / Y-level
 * indicator. Slime chunk detection needs the world seed: the user can enter it
 * (visible in F3 on singleplayer or from the server owner); without a seed
 * slime display is skipped.
 */
public class BiomeOverlay extends Module {
	private final BoolSetting showBiome = new BoolSetting("Biome", true);
	private final BoolSetting showSlime = new BoolSetting("Slime chunks (needs seed)", true);
	private final StringSetting seed = new StringSetting("World seed", "");
	private final BoolSetting showYLevel = new BoolSetting("Y level / bedrock layer", true);

	public BiomeOverlay() {
		super("Biome Overlay", Category.DETECTION, "Biome, slime chunk and Y-level HUD overlay.");
		addSettings(showBiome, showSlime, seed, showYLevel);
	}

	@Override
	public void onHudRender(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		var font = mc.font;
		int x = 6;
		int y = 6;
		BlockPos pos = mc.player.blockPosition();

		if (showBiome.isOn()) {
			Holder<Biome> biomeHolder = mc.level.getBiome(pos);
			String biomeName = biomeHolder.unwrapKey()
					.map(key -> key.identifier().getPath())
					.orElse("unknown");
			graphics.drawString(font, Component.literal("Biome: " + biomeName), x, y, 0xFFB9C8D6);
			y += font.lineHeight + 2;
		}
		if (showYLevel.isOn()) {
			int minY = mc.level.getMinY();
			int maxY = mc.level.getMaxY();
			int bedrockLayer = pos.getY() - minY; // 0 = bedrock floor
			graphics.drawString(font, Component.literal("Y: " + pos.getY() + "  (" + bedrockLayer + " above bottom, max " + maxY + ")"),
					x, y, 0xFFB9C8D6);
			y += font.lineHeight + 2;
		}
		if (showSlime.isOn() && !seed.get().isBlank()) {
			try {
				long seedValue = Long.parseLong(seed.get().trim());
				ChunkPos chunk = mc.player.chunkPosition();
				boolean slime = isSlimeChunk(seedValue, chunk.x, chunk.z);
				String text = "Slime chunk here: " + (slime ? "YES" : "no");
				graphics.drawString(font, Component.literal(text), x, y, slime ? 0xFF7BE38B : 0xFFB9C8D6);
				y += font.lineHeight + 2;
			} catch (NumberFormatException ignored) {
			}
		}
	}

	/** Vanilla slime chunk algorithm (client-side replication of WorldChunk.checkSlimeSpawn rules). */
	public static boolean isSlimeChunk(long worldSeed, int chunkX, int chunkZ) {
		RandomSource random = new LegacyRandomSource(worldSeed
				+ (long) (chunkX * chunkX * 0x4c1906)
				+ (long) (chunkX * 0x5ac0db)
				+ (long) (chunkZ * chunkZ) * 0x4307a7L
				+ (long) (chunkZ * 0x5f24f)
				^ 0x3ad8025fL);
		return random.nextInt(10) == 0;
	}

}
