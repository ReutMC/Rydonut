package dev.rydonut;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.util.FpsTracker;
import dev.rydonut.util.KeyInput;
import dev.rydonut.util.Notifications;
import dev.rydonut.util.TpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Rydonut — client-side visual awareness utility for Minecraft 1.21.11 (Fabric).
 *
 * <p>All features are passive and visual: the mod only reads client-side state
 * (loaded chunks, block entities, entity list, key input) and draws overlays.
 * It never sends extra packets and never modifies outgoing packets.
 */
public class Rydonut implements ClientModInitializer {
	public static final String MOD_ID = "rydonut";
	public static final String VERSION = "0.1.0";

	@Override
	public void onInitializeClient() {
		ConfigManager.init();
		ModuleManager.get().initialize();
		KeyInput.init();
		dev.rydonut.commands.RydonutCommands.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			FpsTracker.frame();
			ModuleManager.get().onTick();
			TpsTracker.tick(client);
		});

		// World rendering: ESP shapes are drawn in the translucent phase so they
		// blend correctly with terrain. Uses the verified 1.21.11 Fabric API.
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
			Minecraft client = Minecraft.getInstance();
			if (client.level == null || client.player == null) {
				return;
			}
			for (var module : ModuleManager.get().getModules()) {
				if (module.isEnabled()) {
					module.onWorldRender(context);
				}
			}
		});

		// HUD: dispatch to all enabled modules (HUD elements + notifications).
		HudRenderCallback.EVENT.register((GuiGraphics graphics, DeltaTracker tickCounter) -> {
			for (var module : ModuleManager.get().getModules()) {
				if (module.isEnabled()) {
					module.onHudRender(graphics, tickCounter);
				}
			}
			renderNotifications(graphics);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.shutdown());
	}

	private static void renderNotifications(GuiGraphics graphics) {
		var font = Minecraft.getInstance().font;
		int width = graphics.guiWidth();
		int y = 6;
		for (Notifications.Toast toast : Notifications.current()) {
			String title = "§b" + toast.title;
			String message = toast.message;
			int w = Math.max(font.width(title), font.width(message)) + 10;
			float progress = toast.progress();
			int alpha = progress < 0.85f ? 0xE6 : (int) (0xE6 * (1.0f - (progress - 0.85f) / 0.15f));
			int bg = (alpha << 24) | 0x0D1B2A;
			graphics.fill(width - w - 4, y, width - 4, y + font.lineHeight * 2 + 6, bg);
			graphics.fill(width - 6, y, width - 4, y + font.lineHeight * 2 + 6, Notifications.accent());
			graphics.drawString(font, title, width - w + 1, y + 3, 0xFF81D4FA);
			graphics.drawString(font, message, width - w + 1, y + 3 + font.lineHeight, 0xFFFFFFFF);
			y += font.lineHeight * 2 + 8;
		}
	}
}
