package dev.rydonut.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.rydonut.config.ConfigManager;
import dev.rydonut.core.Module;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.modules.visuals.Waypoints;
import dev.rydonut.util.ChunkScanService;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/**
 * Client-side /rydonut command: gui, toggle, reset, debug, config, waypoint.
 * Registered via Fabric's client command API — runs entirely client-side.
 */
public final class RydonutCommands {
	private RydonutCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommandManager.literal("rydonut");

			root.then(ClientCommandManager.literal("gui").executes(ctx -> {
				ctx.getSource().getClient().setScreen(new dev.rydonut.gui.ClickGUIScreen());
				return 1;
			}));

			root.then(ClientCommandManager.literal("toggle")
					.then(ClientCommandManager.argument("module", StringArgumentType.string()).executes(ctx -> {
						String name = StringArgumentType.getString(ctx, "module");
						Module module = ModuleManager.get().getByName(name);
						if (module == null) {
							feedback(ctx.getSource(), "No module named '" + name + "'");
							return 0;
						}
						module.toggle();
						feedback(ctx.getSource(), module.getName() + (module.isEnabled() ? " §aenabled" : " §cdisabled"));
						return 1;
					})));

			root.then(ClientCommandManager.literal("list").executes(ctx -> {
				StringBuilder sb = new StringBuilder("Modules: ");
				for (Module module : ModuleManager.get().getModules()) {
					sb.append(module.getName()).append(module.isEnabled() ? "§a*" : "").append("§r, ");
				}
				feedback(ctx.getSource(), sb.toString());
				return 1;
			}));

			root.then(ClientCommandManager.literal("reset").executes(ctx -> {
				for (Module module : ModuleManager.get().getModules()) {
					module.setEnabled(false);
				}
				feedback(ctx.getSource(), "All modules disabled.");
				return 1;
			}));

			root.then(ClientCommandManager.literal("debug")
					.then(ClientCommandManager.literal("basefinder").executes(ctx -> {
						feedback(ctx.getSource(), "ChunkScanService: " + ChunkScanService.debugStats());
						return 1;
					}))
					.then(ClientCommandManager.literal("modules").executes(ctx -> {
						int enabled = 0;
						for (Module module : ModuleManager.get().getModules()) {
							if (module.isEnabled()) {
								enabled++;
							}
						}
						feedback(ctx.getSource(), enabled + "/" + ModuleManager.get().getModules().size() + " modules enabled");
						return 1;
					})));

			root.then(ClientCommandManager.literal("config")
					.then(ClientCommandManager.literal("save").executes(ctx -> {
						ConfigManager.saveProfile(ConfigManager.activeProfile());
						feedback(ctx.getSource(), "Saved profile '" + ConfigManager.activeProfile() + "'");
						return 1;
					}))
					.then(ClientCommandManager.literal("load").executes(ctx -> {
						ConfigManager.loadProfile(ConfigManager.activeProfile());
						feedback(ctx.getSource(), "Loaded profile '" + ConfigManager.activeProfile() + "'");
						return 1;
					}))
					.then(ClientCommandManager.literal("export").executes(ctx -> {
						ConfigManager.exportProfile(ConfigManager.activeProfile());
						feedback(ctx.getSource(), "Exported to " + ConfigManager.configDir().toAbsolutePath());
						return 1;
					}))
					.then(ClientCommandManager.literal("import").executes(ctx -> {
						ConfigManager.importLatestExport(ConfigManager.activeProfile());
						feedback(ctx.getSource(), "Imported latest export.");
						return 1;
					})));

			root.then(ClientCommandManager.literal("waypoint")
					.then(ClientCommandManager.literal("add")
							.then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(ctx -> {
								String name = StringArgumentType.getString(ctx, "name");
								dev.rydonut.config.WaypointStore.Point point = Waypoints.addHere(ctx.getSource().getClient(), name);
								if (point == null) {
									feedback(ctx.getSource(), "Not in a world.");
									return 0;
								}
								feedback(ctx.getSource(), "Waypoint '" + name + "' added.");
								return 1;
							})))
					.then(ClientCommandManager.literal("remove")
							.then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(ctx -> {
								String name = StringArgumentType.getString(ctx, "name");
								var source = ctx.getSource();
								String dimension = source.getWorld() == null ? "?" : source.getWorld().dimension().identifier().toString();
								WaypointStoreHelper.remove(name, dimension);
								feedback(source, "Waypoint '" + name + "' removed (if it existed).");
								return 1;
							}))));

			dispatcher.register(root);
		});
	}

	private static void feedback(FabricClientCommandSource source, String message) {
		source.sendFeedback(net.minecraft.network.chat.Component.literal("§b[Rydonut]§r " + message));
	}

	/** Small indirection so this file does not import the store twice. */
	private static final class WaypointStoreHelper {
		static void remove(String name, String dimension) {
			dev.rydonut.config.WaypointStore.remove(name, dimension);
		}
	}
}
