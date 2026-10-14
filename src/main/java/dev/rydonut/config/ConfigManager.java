package dev.rydonut.config;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.core.ModuleManager;
import dev.rydonut.settings.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * JSON config persistence.
 *
 * <p>Files live in <code>.minecraft/rydonut/</code>:
 * <ul>
 *   <li><code>profiles/&lt;name&gt;.json</code> — full module + settings state</li>
 *   <li><code>waypoints.json</code> — waypoints shared by the Waypoints module</li>
 *   <li><code>finds/&lt;timestamp&gt;.txt</code> — BaseFinder exported positions</li>
 * </ul>
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	private static volatile boolean dirty;
	private static volatile String activeProfile = "default";
	private static final ScheduledExecutorService AUTOSAVE = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Rydonut-Config-Autosave");
		t.setDaemon(true);
		return t;
	});

	private ConfigManager() {
	}

	public static Path configDir() {
		return FabricLoader.getInstance().getGameDir().resolve("rydonut");
	}

	public static Path profilesDir() {
		return configDir().resolve("profiles");
	}

	public static Path findsDir() {
		return configDir().resolve("finds");
	}

	public static Path waypointsFile() {
		return configDir().resolve("waypoints.json");
	}

	public static String activeProfile() {
		return activeProfile;
	}

	public static void markDirty() {
		dirty = true;
	}

	public static void init() {
		try {
			Files.createDirectories(profilesDir());
			Files.createDirectories(findsDir());
		} catch (IOException e) {
			RydonutLogger.warn("Could not create config dirs: " + e);
		}
		AUTOSAVE.scheduleWithFixedDelay(() -> {
			if (dirty) {
				dirty = false;
				saveProfile(activeProfile);
			}
		}, 5, 5, TimeUnit.SECONDS);
		loadProfile(activeProfile);
		WaypointStore.load();
	}

	public static void shutdown() {
		saveProfile(activeProfile);
		WaypointStore.save();
		AUTOSAVE.shutdownNow();
	}

	// ---------------------------------------------------------------- profiles

	private static final class Snapshot {
		String activeProfile;
		Map<String, ModuleSnapshot> modules = new LinkedHashMap<>();
	}

	private static final class ModuleSnapshot {
		boolean enabled;
		int keybind;
		Map<String, String> settings = new LinkedHashMap<>();
	}

	public static synchronized void saveProfile(String name) {
		Snapshot snap = new Snapshot();
		snap.activeProfile = name;
		for (Module module : ModuleManager.get().getModules()) {
			ModuleSnapshot ms = new ModuleSnapshot();
			ms.enabled = module.isEnabled();
			ms.keybind = module.getKeybind().get();
			for (Setting<?> setting : module.getSettings()) {
				ms.settings.put(setting.getName(), setting.serializeValue());
			}
			snap.modules.put(module.getName(), ms);
		}
		try {
			Path file = profilesDir().resolve(sanitize(name) + ".json");
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(snap), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			RydonutLogger.warn("Failed to save profile '" + name + "': " + e);
		}
	}

	public static synchronized void loadProfile(String name) {
		Path file = profilesDir().resolve(sanitize(name) + ".json");
		if (!Files.exists(file)) {
			activeProfile = name;
			saveProfile(name);
			return;
		}
		try {
			Snapshot snap = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Snapshot.class);
			if (snap == null || snap.modules == null) {
				return;
			}
			for (Module module : ModuleManager.get().getModules()) {
				ModuleSnapshot ms = snap.modules.get(module.getName());
				if (ms == null) {
					continue;
				}
				module.setEnabled(false);
				module.getKeybind().deserializeValue(String.valueOf(ms.keybind));
				if (ms.settings != null) {
					for (Setting<?> setting : module.getSettings()) {
						String raw = ms.settings.get(setting.getName());
						if (raw != null) {
							setting.deserializeValue(raw);
						}
					}
				}
				module.setEnabled(ms.enabled);
			}
			activeProfile = name;
			RydonutLogger.info("Loaded profile '" + name + "'");
		} catch (IOException e) {
			RydonutLogger.warn("Failed to load profile '" + name + "': " + e);
		}
	}

	public static void newProfile(String name) {
		saveProfile(activeProfile);
		activeProfile = sanitize(name);
		saveProfile(activeProfile);
	}

	public static void exportProfile(String name) {
		Path src = profilesDir().resolve(sanitize(name) + ".json");
		Path dst = configDir().resolve("export_" + sanitize(name) + "_" + LocalDateTime.now().format(STAMP) + ".json");
		try {
			Files.createDirectories(configDir());
			Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
			RydonutLogger.info("Exported profile to " + dst.toAbsolutePath());
		} catch (IOException e) {
			RydonutLogger.warn("Profile export failed: " + e);
		}
	}

	public static void importLatestExport(String name) {
		try {
			Path dir = configDir();
			if (!Files.isDirectory(dir)) {
				return;
			}
			Path best = null;
			try (var stream = Files.list(dir)) {
				for (Path p : stream.filter(p -> p.getFileName().toString().startsWith("export_")).toList()) {
					if (best == null || p.getFileName().toString().compareTo(best.getFileName().toString()) > 0) {
						best = p;
					}
				}
			}
			if (best == null) {
				RydonutLogger.info("No profile export found in " + dir.toAbsolutePath());
				return;
			}
			Files.copy(best, profilesDir().resolve(sanitize(name) + ".json"), StandardCopyOption.REPLACE_EXISTING);
			loadProfile(name);
			RydonutLogger.info("Imported profile from " + best.getFileName());
		} catch (IOException e) {
			RydonutLogger.warn("Profile import failed: " + e);
		}
	}

	private static String sanitize(String name) {
		String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
		return s.isBlank() ? "default" : s;
	}

	// ---------------------------------------------------------------- finds export

	public static synchronized void exportFind(String dimension, String line) {
		try {
			Files.createDirectories(findsDir());
			Path file = findsDir().resolve("finds_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt");
			String stamped = LocalDateTime.now().format(STAMP) + " [" + dimension + "] " + line + System.lineSeparator();
			Files.writeString(file, stamped, StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			RydonutLogger.warn("Find export failed: " + e);
		}
	}

	public static Category categoryOf(String title) {
		for (Category c : Category.values()) {
			if (c.title.equals(title)) {
				return c;
			}
		}
		return Category.VISUALS;
	}

	// tiny local logger to avoid depending on slf4j config
	public static final class RydonutLogger {
		private static final String PREFIX = "[Rydonut] ";

		public static void info(String msg) {
			System.out.println(PREFIX + msg);
		}

		public static void warn(String msg) {
			System.out.println(PREFIX + "WARN: " + msg);
		}
	}

	@SuppressWarnings("unused")
	private static final TypeToken<Snapshot> SNAPSHOT_TYPE = new TypeToken<>() {
	};
}
