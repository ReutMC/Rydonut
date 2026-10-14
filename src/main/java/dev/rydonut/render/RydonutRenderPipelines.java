package dev.rydonut.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Rydonut render pipelines.
 *
 * <p>Both pipelines use the vanilla {@code position_color} quad shader
 * (via {@link RenderPipelines#DEBUG_FILLED_SNIPPET}) and differ only in depth
 * testing:
 * <ul>
 *   <li>{@link #FILLED} — depth tested (hidden behind walls)</li>
 *   <li>{@link #FILLED_THROUGH_WALLS} — no depth test (visible through terrain)</li>
 * </ul>
 * All geometry (fills, box edges, tracers) is drawn as position-color quads,
 * so a single shader covers everything and thin "lines" are rendered as
 * elongated cuboids.
 *
 * <p>Verified against Minecraft 1.21.11 APIs:
 * {@link RenderPipeline#builder(RenderPipeline.Snippet...)},
 * {@link RenderPipelines#register(RenderPipeline)},
 * {@link RenderSetup#builder(RenderPipeline)},
 * {@link RenderType#create(String, RenderSetup)}.
 */
public final class RydonutRenderPipelines {
	private static final Identifier FILLED_ID = Identifier.fromNamespaceAndPath("rydonut", "pipeline/filled");
	private static final Identifier FILLED_THROUGH_WALLS_ID = Identifier.fromNamespaceAndPath("rydonut", "pipeline/filled_through_walls");

	public static final RenderPipeline FILLED_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(FILLED_ID)
			.withCull(false)
			.withDepthWrite(false)
			.build());

	public static final RenderPipeline FILLED_THROUGH_WALLS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(FILLED_THROUGH_WALLS_ID)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withCull(false)
			.withDepthWrite(false)
			.withBlend(BlendFunction.TRANSLUCENT)
			.build());

	public static final RenderType FILLED = RenderType.create("rydonut_filled",
			RenderSetup.builder(FILLED_PIPELINE).bufferSize(0x200000).createRenderSetup());

	public static final RenderType FILLED_THROUGH_WALLS = RenderType.create("rydonut_filled_through_walls",
			RenderSetup.builder(FILLED_THROUGH_WALLS_PIPELINE).bufferSize(0x200000).createRenderSetup());

	private RydonutRenderPipelines() {
	}
}
