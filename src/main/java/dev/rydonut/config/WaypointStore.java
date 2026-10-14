package dev.rydonut.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Client-side waypoint storage, persisted to rydonut/waypoints.json.
 */
public final class WaypointStore {
        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        public static final class Point {
                public String name;
                public double x;
                public double y;
                public double z;
                public String dimension;
                public int color;

                public Point() {
                }

                public Point(String name, double x, double y, double z, String dimension, int color) {
                        this.name = name;
                        this.x = x;
                        this.y = y;
                        this.z = z;
                        this.dimension = dimension;
                        this.color = color;
                }
        }

        private static final Map<String, Point> POINTS = new ConcurrentHashMap<>();

        private WaypointStore() {
        }

        public static List<Point> all() {
                return new ArrayList<>(POINTS.values());
        }

        public static void add(Point point) {
                POINTS.put(point.name.toLowerCase() + "@" + point.dimension, point);
                save();
        }

        public static void remove(String name, String dimension) {
                POINTS.remove(name.toLowerCase() + "@" + dimension);
                save();
        }

        public static boolean exists(String name, String dimension) {
                return POINTS.containsKey(name.toLowerCase() + "@" + dimension);
        }

        private static String dimensionKey(net.minecraft.world.level.Level level) {
                return level.dimension().identifier().toString();
        }

        public static void load() {
                try {
                        if (!java.nio.file.Files.exists(ConfigManager.waypointsFile())) {
                                return;
                        }
                        String json = java.nio.file.Files.readString(ConfigManager.waypointsFile());
                        List<Point> points = GSON.fromJson(json, new TypeToken<List<Point>>() {
                        }.getType());
                        POINTS.clear();
                        if (points != null) {
                                for (Point p : points) {
                                        POINTS.put(p.name.toLowerCase() + "@" + p.dimension, p);
                                }
                        }
                } catch (Exception e) {
                        ConfigManager.RydonutLogger.warn("Waypoints load failed: " + e);
                }
        }

        public static void save() {
                try {
                        java.nio.file.Files.createDirectories(ConfigManager.configDir());
                        java.nio.file.Files.writeString(ConfigManager.waypointsFile(), GSON.toJson(all()));
                } catch (Exception e) {
                        ConfigManager.RydonutLogger.warn("Waypoints save failed: " + e);
                }
        }
}
