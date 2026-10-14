package dev.rydonut.core;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.rydonut.render.Colors;
import dev.rydonut.settings.KeybindSetting;
import dev.rydonut.settings.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for every Rydonut module.
 *
 * <p>Modules are purely visual / passive: they only read client-side data
 * (loaded chunks, block states, block entities, the entity list, incoming
 * packets as reflected in the client world). They never send extra packets
 * and never modify outgoing packets.
 */
public abstract class Module {
        protected final Minecraft mc = Minecraft.getInstance();

        private final String name;
        private final String description;
        private final Category category;
        private final List<Setting<?>> settings = new ArrayList<>();
        private final KeybindSetting keybind;

        private boolean enabled;
        private float animation; // 0..1, used by the GUI and the ArrayList

        protected Module(String name, Category category, String description) {
                this.name = name;
                this.category = category;
                this.description = description;
                this.keybind = new KeybindSetting("Keybind", 0);
        }

        // ---------------------------------------------------------------- lifecycle

        public void toggle() {
                setEnabled(!enabled);
        }

        public void setEnabled(boolean enabled) {
                if (this.enabled == enabled) {
                        return;
                }
                this.enabled = enabled;
                if (enabled) {
                        onEnable();
                } else {
                        onDisable();
                }
        }

        protected void onEnable() {
        }

        protected void onDisable() {
        }

        /** Called every client tick while the module is enabled. */
        public void onTick() {
        }

        /** Called during world rendering while the module is enabled. */
        public void onWorldRender(WorldRenderContext context) {
        }

        /** Called every frame for HUD rendering while the module is enabled. */
        public void onHudRender(GuiGraphics graphics, DeltaTracker tickCounter) {
        }

        // ---------------------------------------------------------------- settings

        protected void addSettings(Setting<?>... entries) {
                for (Setting<?> s : entries) {
                        settings.add(s);
                }
        }

        /** Registers the keybind setting at the end so it always appears last. */
        public void finishSetup() {
                settings.add(keybind);
                keybind.onChange(v -> dev.rydonut.config.ConfigManager.markDirty());
        }

        // ---------------------------------------------------------------- getters

        public String getName() {
                return name;
        }

        public String getDescription() {
                return description;
        }

        public Category getCategory() {
                return category;
        }

        public List<Setting<?>> getSettings() {
                return settings;
        }

        @Nullable
        public <T extends Setting<?>> T getSetting(String settingName, Class<T> type) {
                for (Setting<?> s : settings) {
                        if (s.getName().equals(settingName) && type.isInstance(s)) {
                                return type.cast(s);
                        }
                }
                return null;
        }

        public KeybindSetting getKeybind() {
                return keybind;
        }

        public boolean isEnabled() {
                return enabled;
        }

        public float getAnimation() {
                return animation;
        }

        public void setAnimation(float animation) {
                this.animation = animation;
        }

        /**
         * Whether pressing the keybind should toggle the module (true) or the key is
         * meant to be held (false, e.g. Zoom).
         */
        public boolean togglesOnPress() {
                return true;
        }

        /** Accent color used in the ArrayList for this module. */
        public int getListColor() {
                return Colors.PRIMARY;
        }

        @Nullable
        public String getHudText() {
                return null;
        }
}
