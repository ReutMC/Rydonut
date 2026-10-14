package dev.rydonut.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.rydonut.util.KeyInput;
import net.minecraft.client.Minecraft;

/**
 * Holds and drives all modules.
 */
public class ModuleManager {
        private static final ModuleManager INSTANCE = new ModuleManager();

        private final List<Module> modules = new ArrayList<>();
        private List<Module> activeSorted = new ArrayList<>();
        private boolean listDirty = true;

        public static ModuleManager get() {
                return INSTANCE;
        }

        private void register(Module module) {
                module.finishSetup();
                modules.add(module);
        }

        public void initialize() {
                // Visuals
                register(new dev.rydonut.modules.visuals.PlayerEsp());
                register(new dev.rydonut.modules.visuals.StorageEsp());
                register(new dev.rydonut.modules.visuals.ItemEsp());
                register(new dev.rydonut.modules.visuals.EntityEsp());
                register(new dev.rydonut.modules.visuals.BlockEsp());
                register(new dev.rydonut.modules.visuals.Tracers());
                register(new dev.rydonut.modules.visuals.Chams());
                register(new dev.rydonut.modules.visuals.Nametags());
                register(new dev.rydonut.modules.visuals.TrajectoryEsp());
                register(new dev.rydonut.modules.visuals.ChunkBorders());
                register(new dev.rydonut.modules.visuals.Waypoints());
                register(new dev.rydonut.modules.visuals.HoleVoidEsp());
                // Detection
                register(new dev.rydonut.modules.detection.BaseFinder());
                register(new dev.rydonut.modules.detection.NewChunks());
                register(new dev.rydonut.modules.detection.PlayerTracker());
                register(new dev.rydonut.modules.detection.LogoutSpots());
                register(new dev.rydonut.modules.detection.EyeOfEnderHelper());
                register(new dev.rydonut.modules.detection.BiomeOverlay());
                // HUD
                register(new dev.rydonut.hud.WatermarkHud());
                register(new dev.rydonut.hud.FpsHud());
                register(new dev.rydonut.hud.PingHud());
                register(new dev.rydonut.hud.CoordsHud());
                register(new dev.rydonut.hud.TpsHud());
                register(new dev.rydonut.hud.DurabilityHud());
                register(new dev.rydonut.hud.ArrayListHud());
                // Misc
                register(new dev.rydonut.modules.visuals.Fullbright());
                register(new dev.rydonut.modules.visuals.Zoom());
                register(new dev.rydonut.modules.visuals.Freecam());
                register(new dev.rydonut.modules.misc.ClickGuiModule());

                modules.sort((a, b) -> {
                        int c = a.getCategory().ordinal() - b.getCategory().ordinal();
                        return c != 0 ? c : a.getName().compareToIgnoreCase(b.getName());
                });
        }

        public List<Module> getModules() {
                return Collections.unmodifiableList(modules);
        }

        public List<Module> getModulesIn(Category category) {
                List<Module> out = new ArrayList<>();
                for (Module m : modules) {
                        if (m.getCategory() == category) {
                                out.add(m);
                        }
                }
                return out;
        }

        @Nullable
        public Module getByName(String name) {
                for (Module m : modules) {
                        if (m.getName().equalsIgnoreCase(name)) {
                                return m;
                        }
                }
                return null;
        }

        /** Called at the end of every client tick. */
        public void onTick() {
                for (Module m : modules) {
                        if (m.isEnabled()) {
                                m.onTick();
                        }
                }
                // Keybind handling: toggle modules on physical key press.
                for (Module m : modules) {
                        var key = m.getKeybind();
                        if (key.isBound() && !key.isListening() && m.togglesOnPress()) {
                                while (KeyInput.consumePress(key.get())) {
                                        m.toggle();
                                }
                        }
                }
                KeyInput.endTick();
                listDirty = true;
        }

        /** Enabled modules sorted by rendered text width (longest first) for the ArrayList HUD. */
        public List<Module> getActiveSortedByWidth() {
                if (listDirty) {
                        List<Module> active = new ArrayList<>();
                        for (Module m : modules) {
                                if (m.isEnabled()) {
                                        active.add(m);
                                }
                        }
                        int fontWidth = Minecraft.getInstance().font.width("");
                        active.sort((a, b) -> (fontWidth + Minecraft.getInstance().font.width(b.getName()))
                                        - (fontWidth + Minecraft.getInstance().font.width(a.getName())));
                        activeSorted = active;
                        listDirty = false;
                }
                return activeSorted;
        }

        public void markListDirty() {
                listDirty = true;
        }
}
