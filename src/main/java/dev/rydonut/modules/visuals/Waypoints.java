package dev.rydonut.modules.visuals;

import java.util.List;

import dev.rydonut.config.WaypointStore;
import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.util.ChunkScanService;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Waypoints system: persistent client-side markers with beacon-like beams.
 * Add/remove via /rydonut waypoint add|remove <name> and the BaseFinder GUI.
 */
public class Waypoints extends Module {
        private final DoubleSetting range = new DoubleSetting("Range", 512, 32, 2048, 32);
        private final BoolSetting showBeam = new BoolSetting("Beam", true);
        private final BoolSetting showDistance = new BoolSetting("Distance label", true);

        public Waypoints() {
                super("Waypoints", Category.VISUALS, "Renders saved waypoints with beams and labels.");
                addSettings(range, showBeam, showDistance);
        }

        @Override
        public void onWorldRender(WorldRenderContext context) {
                if (mc.player == null || mc.level == null) {
                        return;
                }
                WorldShapes.begin(context);
                try {
                        String dimension = mc.level.dimension().identifier().toString();
                        double rangeSq = range.get() * range.get();
                        List<WaypointStore.Point> points = WaypointStore.all();
                        for (dev.rydonut.config.WaypointStore.Point point : points) {
                                if (!point.dimension.equals(dimension)) {
                                        continue;
                                }
                                double dx = point.x - mc.player.getX();
                                double dy = point.y - mc.player.getY();
                                double dz = point.z - mc.player.getZ();
                                if (dx * dx + dy * dy + dz * dz > rangeSq) {
                                        continue;
                                }
                                int color = point.color == 0 ? Colors.PRIMARY : point.color;
                                // marker box
                                WorldShapes.filledBox(point.x - 0.5, point.y - 0.5, point.z - 0.5,
                                                point.x + 0.5, point.y + 0.5, point.z + 0.5, Colors.withAlpha(color, 0.35f), true);
                                WorldShapes.outlinedBox(point.x - 0.5, point.y - 0.5, point.z - 0.5,
                                                point.x + 0.5, point.y + 0.5, point.z + 0.5, color, true);
                                // beam
                                if (showBeam.isOn()) {
                                        WorldShapes.line(point.x, point.y, point.z, point.x, point.y + 48, point.z, Colors.withAlpha(color, 0.55f), true);
                                        WorldShapes.line(point.x, point.y, point.z, point.x, point.y - 24, point.z, Colors.withAlpha(color, 0.35f), true);
                                }
                                String label = point.name;
                                if (showDistance.isOn()) {
                                        int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
                                        label += " [" + dist + "m]";
                                }
                                WorldShapes.text(label, point.x, point.y + 1.0, point.z, 1.1f, Colors.WHITE, 0x90000000, true);
                        }
                } finally {
                        WorldShapes.end();
                }
        }

        /** Adds a waypoint at the player's position. Returns the created point. */
        public static WaypointStore.Point addHere(Minecraft mc, String name) {
                if (mc.player == null || mc.level == null) {
                        return null;
                }
                WaypointStore.Point point = new WaypointStore.Point(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                mc.level.dimension().identifier().toString(), Colors.PRIMARY);
                WaypointStore.add(point);
                return point;
        }

        /** Adds a waypoint at a chunk's center (used by the BaseFinder "click to waypoint" flow). */
        public static void addChunk(long chunkPos, String name, int color) {
                ChunkPos pos = new ChunkPos(chunkPos);
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                        return;
                }
                WaypointStore.Point point = new WaypointStore.Point(name, pos.getMinBlockX() + 8.0, mc.level.getMinY() + 40, pos.getMinBlockZ() + 8.0,
                                mc.level.dimension().identifier().toString(), color);
                WaypointStore.add(point);
        }
}
