package dev.rydonut.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

/**
 * World-space shape drawing used by every ESP module.
 *
 * <p>All coordinates are absolute world coordinates; they are converted to
 * camera-relative space here (required by the world renderer's buffered
 * consumers). Shapes are position-color quads; thin "lines" are thin cuboids
 * so a single quad pipeline covers fills, outlines, tracers and chunk borders.
 */
public final class WorldShapes {
	private static final float LINE_THICKNESS = 0.022f;
	private static final int FULL_LIGHT = 15728880;

	private static Matrix4f positionMatrix;
	private static MultiBufferSource consumers;
	private static double camX, camY, camZ;
	private static boolean active;

	private WorldShapes() {
	}

	/** Prepares the static draw state at the start of a world render callback. */
	public static void begin(WorldRenderContext context) {
		positionMatrix = context.matrices().last().pose();
		consumers = context.consumers();
		Vec3 cam = context.worldState().cameraRenderState.pos;
		camX = cam.x;
		camY = cam.y;
		camZ = cam.z;
		active = true;
	}

	public static void end() {
		active = false;
		consumers = null;
		positionMatrix = null;
	}

	public static boolean isActive() {
		return active;
	}

	// ---------------------------------------------------------------- boxes

	/** Filled cuboid (6 faces, two triangles' worth of quad winding per face). */
	public static void filledBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean throughWalls) {
		if (!active) {
			return;
		}
		VertexConsumer buffer = consumers.getBuffer(throughWalls ? RydonutRenderPipelines.FILLED_THROUGH_WALLS : RenderTypes.debugFilledBox());
		int c = color;
		// Front (-Z)
		quad(buffer, c, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1);
		// Back (+Z)
		quad(buffer, c, x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2);
		// Left (-X)
		quad(buffer, c, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2);
		// Right (+X)
		quad(buffer, c, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1);
		// Top (+Y)
		quad(buffer, c, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2);
		// Bottom (-Y)
		quad(buffer, c, x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1);
	}

	/** Box outline drawn as 12 thin cuboids (works with the quad pipeline + through walls). */
	public static void outlinedBox(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean throughWalls) {
		if (!active) {
			return;
		}
		// Bottom rectangle
		line(x1, y1, z1, x2, y1, z1, color, throughWalls);
		line(x2, y1, z1, x2, y1, z2, color, throughWalls);
		line(x2, y1, z2, x1, y1, z2, color, throughWalls);
		line(x1, y1, z2, x1, y1, z1, color, throughWalls);
		// Top rectangle
		line(x1, y2, z1, x2, y2, z1, color, throughWalls);
		line(x2, y2, z1, x2, y2, z2, color, throughWalls);
		line(x2, y2, z2, x1, y2, z2, color, throughWalls);
		line(x1, y2, z2, x1, y2, z1, color, throughWalls);
		// Vertical edges
		line(x1, y1, z1, x1, y2, z1, color, throughWalls);
		line(x2, y1, z1, x2, y2, z1, color, throughWalls);
		line(x2, y1, z2, x2, y2, z2, color, throughWalls);
		line(x1, y1, z2, x1, y2, z2, color, throughWalls);
	}

	// ---------------------------------------------------------------- lines

	/** Straight line between two points, rendered as a thin cuboid (prism). */
	public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int color, boolean throughWalls) {
		if (!active) {
			return;
		}
		double dx = x2 - x1;
		double dy = y2 - y1;
		double dz = z2 - z1;
		double lenSq = dx * dx + dy * dy + dz * dz;
		if (lenSq < 1.0e-8) {
			return;
		}
		double inv = 1.0 / Math.sqrt(lenSq);
		double nx = dx * inv, ny = dy * inv, nz = dz * inv;

		// u = n x (0,1,0) normalized unless the line is vertical, then n x (1,0,0)
		double ux, uy, uz;
		if (Math.abs(ny) < 0.99) {
			ux = nz;
			uy = 0.0;
			uz = -nx;
			double l = Math.sqrt(ux * ux + uz * uz);
			ux /= l;
			uz /= l;
		} else {
			ux = 0.0;
			uy = 0.0;
			uz = 1.0;
		}
		// v = n x u
		double vx = ny * uz - nz * uy;
		double vy = nz * ux - nx * uz;
		double vz = nx * uy - ny * ux;

		double t = LINE_THICKNESS;
		// 8 corners: A +/- u*t +/- v*t and B +/- u*t +/- v*t
		double a_uu = x1 + ux * t, a_uv = y1 + uy * t, a_uw = z1 + uz * t;
		double a_uu2 = x1 - ux * t, a_uv2 = y1 - uy * t, a_uw2 = z1 - uz * t;
		double b_uu = x2 + ux * t, b_uv = y2 + uy * t, b_uw = z2 + uz * t;
		double b_uu2 = x2 - ux * t, b_uv2 = y2 - uy * t, b_uw2 = z2 - uz * t;
		double a_vu = x1 + vx * t, a_vv = y1 + vy * t, a_vw = z1 + vz * t;
		double a_vu2 = x1 - vx * t, a_vv2 = y1 - vy * t, a_vw2 = z1 - vz * t;
		double b_vu = x2 + vx * t, b_vv = y2 + vy * t, b_vw = z2 + vz * t;
		double b_vu2 = x2 - vx * t, b_vv2 = y2 - vy * t, b_vw2 = z2 - vz * t;

		VertexConsumer buffer = consumers.getBuffer(throughWalls ? RydonutRenderPipelines.FILLED_THROUGH_WALLS : RenderTypes.debugFilledBox());
		int c = color;
		// side +u
		quad(buffer, c, a_uu - vx * t, a_uv - vy * t, a_uw - vz * t, b_uu - vx * t, b_uv - vy * t, b_uw - vz * t,
				b_uu + vx * t, b_uv + vy * t, b_uw + vz * t, a_uu + vx * t, a_uv + vy * t, a_uw + vz * t);
		// side -u
		quad(buffer, c, a_uu2 - vx * t, a_uv2 - vy * t, a_uw2 - vz * t, b_uu2 - vx * t, b_uv2 - vy * t, b_uw2 - vz * t,
				b_uu2 + vx * t, b_uv2 + vy * t, b_uw2 + vz * t, a_uu2 + vx * t, a_uv2 + vy * t, a_uw2 + vz * t);
		// side +v
		quad(buffer, c, a_vu - ux * t, a_vv - uy * t, a_vw - uz * t, b_vu - ux * t, b_vv - uy * t, b_vw - uz * t,
				b_vu + ux * t, b_vv + uy * t, b_vw + uz * t, a_vu + ux * t, a_vv + uy * t, a_vw + uz * t);
		// side -v
		quad(buffer, c, a_vu2 - ux * t, a_vv2 - uy * t, a_vw2 - uz * t, b_vu2 - ux * t, b_vv2 - uy * t, b_vw2 - uz * t,
				b_vu2 + ux * t, b_vv2 + uy * t, b_vw2 + uz * t, a_vu2 + ux * t, a_vv2 + uy * t, a_vw2 + uz * t);
		// cap A
		quad(buffer, c, a_uu + vx * t, a_uv + vy * t, a_uw + vz * t, a_uu2 + vx * t, a_uv2 + vy * t, a_uw2 + vz * t,
				a_uu2 - vx * t, a_uv2 - vy * t, a_uw2 - vz * t, a_uu - vx * t, a_uv - vy * t, a_uw - vz * t);
		// cap B
		quad(buffer, c, b_uu + vx * t, b_uv + vy * t, b_uw + vz * t, b_uu2 + vx * t, b_uv2 + vy * t, b_uw2 + vz * t,
				b_uu2 - vx * t, b_uv2 - vy * t, b_uw2 - vz * t, b_uu - vx * t, b_uv - vy * t, b_uw - vz * t);
	}

	// ---------------------------------------------------------------- text

	/**
	 * Camera-facing text label at a world position.
	 *
	 * @param scale       text scale (1.0 ~ vanilla nametag size)
	 * @param background  ARGB background color, 0 = none
	 * @param seeThrough  render through walls
	 */
	public static void text(String text, double x, double y, double z, float scale, int color, int background, boolean seeThrough) {
		if (!active || text == null || text.isEmpty() || consumers == null) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		double relX = x - camX;
		double relY = y - camY;
		double relZ = z - camZ;
		if (relX * relX + relY * relY + relZ * relZ > 6400.0 * 6400.0) {
			return; // cull beyond 80 chunks
		}
		Camera camera = mc.gameRenderer.getMainCamera();
		Quaternionf rotation = camera.rotation();
		PoseStack matrices = new PoseStack();
		matrices.mulPose(positionMatrix);
		matrices.translate(relX, relY, relZ);
		matrices.mulPose(rotation);
		matrices.scale(-scale * 0.025f, -scale * 0.025f, scale * 0.025f);
		Matrix4f matrix = matrices.last().pose();
		float halfWidth = font.width(text) / 2.0f;
		font.drawInBatch(text, -halfWidth, 0.0f, color, false, matrix, consumers,
				seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, FULL_LIGHT);
	}

	// ---------------------------------------------------------------- chunk borders

	/** Chunk border outline for the given chunk column. */
	public static void chunkBorder(int chunkX, int chunkZ, double yMin, double yMax, int color) {
		double x1 = chunkX * 16 + 0.002;
		double z1 = chunkZ * 16 + 0.002;
		double x2 = chunkX * 16 + 16 - 0.002;
		double z2 = chunkZ * 16 + 16 - 0.002;
		outlinedBox(x1, yMin, z1, x2, yMax, z2, color, true);
	}

	// ---------------------------------------------------------------- internals

	private static void quad(VertexConsumer buffer, int color,
			double x1, double y1, double z1,
			double x2, double y2, double z2,
			double x3, double y3, double z3,
			double x4, double y4, double z4) {
		addVertex(buffer, color, x1, y1, z1);
		addVertex(buffer, color, x2, y2, z2);
		addVertex(buffer, color, x3, y3, z3);
		addVertex(buffer, color, x4, y4, z4);
	}

	private static void addVertex(VertexConsumer buffer, int color, double x, double y, double z) {
		buffer.addVertex(positionMatrix,
				(float) (x - camX),
				(float) (y - camY),
				(float) (z - camZ)).setColor(color);
	}
}
