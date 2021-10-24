/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.rendercontext;

import static grondag.canvas.buffer.format.EncoderUtils.colorizeQuad;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.base.renderer.context.BaseBlockContext;
import io.vram.frex.base.renderer.mesh.BaseQuadEmitter;

import grondag.canvas.buffer.input.VertexCollectorList;
import grondag.canvas.config.Configurator;
import grondag.canvas.light.AoCalculator;
import grondag.canvas.light.LightSmoother;
import grondag.canvas.material.state.CanvasRenderMaterial;
import grondag.canvas.render.terrain.TerrainFormat;
import grondag.canvas.terrain.region.input.InputRegion;
import grondag.canvas.terrain.region.input.PackedInputRegion;
import grondag.canvas.terrain.util.RenderRegionStateIndexer;

/**
 * Implementation of RenderContext used during terrain rendering.
 * Dispatches calls from models during chunk rebuild to the appropriate consumer,
 * and holds/manages all of the state needed by them.
 */
public class TerrainRenderContext extends AbstractBlockRenderContext<InputRegion> {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();
	public final InputRegion region;
	public final MatrixStack matrixStack = MatrixStack.cast(new PoseStack());

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected int ao(int cacheIndex) {
			return region.cachedAoLevel(cacheIndex);
		}

		@Override
		protected int brightness(int cacheIndex) {
			return region.cachedBrightness(cacheIndex);
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			return region.isClosed(cacheIndex);
		}
	};

	public TerrainRenderContext() {
		super("TerrainRenderContext");
		region = new InputRegion(this);
		inputContext.prepareForWorld(region, true, matrixStack);
		collectors = new VertexCollectorList(true, true);
	}

	@Override
	protected BaseBlockContext<InputRegion> createInputContext() {
		return new BaseBlockContext<>() {
			@Override
			protected int fastBrightness(BlockPos pos) {
				return region.cachedBrightness(pos);
			}

			@Override
			public @Nullable Object blockEntityRenderData(BlockPos pos) {
				return region.getBlockEntityRenderAttachment(pos);
			}
		};
	}

	public TerrainRenderContext prepareForRegion(PackedInputRegion protoRegion) {
		nonCullBlockEntities.clear();
		addedBlockEntities.clear();
		removedBlockEntities.clear();
		region.prepare(protoRegion);
		animationBits.clear();

		if (Configurator.lightSmoothing) {
			//            final long start = counter.startRun();
			LightSmoother.computeSmoothedBrightness(region);
		}

		return this;
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model) {
		isFluidModel = true;
		renderInner(blockState, blockPos, defaultAo, model);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model) {
		isFluidModel = false;
		renderInner(blockState, blockPos, defaultAo, model);
	}

	// PERF: don't pass in matrixStack each time, just change model matrix directly
	private void renderInner(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model) {
		try {
			aoCalc.prepare(RenderRegionStateIndexer.interiorIndex(blockPos));
			prepareForBlock(blockState, blockPos, defaultAo);
			model.renderAsBlock(this.inputContext, emitter());
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.forThrowable(var9, "Tesselating block in world - Canvas Renderer");
			final CrashReportCategory crashReportElement_1 = crashReport_1.addCategory("Block being tesselated");
			CrashReportCategory.populateBlockDetails(crashReportElement_1, region, blockPos, blockState);
			throw new ReportedException(crashReport_1);
		}
	}

	@Override
	public int brightness() {
		return 0;
	}

	@Override
	public void computeAo(BaseQuadEmitter quad) {
		aoCalc.compute(quad);
	}

	@Override
	public void computeFlat(BaseQuadEmitter quad) {
		if (Configurator.semiFlatLighting) {
			aoCalc.computeFlat(quad);
		} else if (Configurator.hdLightmaps()) {
			// FEAT: per-vertex light maps will be ignored unless we bake a custom HD map
			// or retain vertex light maps in buffer format and logic in shader to take max
			aoCalc.computeFlatHd(quad, inputContext.flatBrightness(quad));
		} else {
			computeFlatSimple(quad);
		}
	}

	@Override
	protected void encodeQuad(BaseQuadEmitter quad) {
		// needs to happen before offsets are applied
		if (!quad.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			computeAo(quad);
		} else {
			computeFlat(quad);
		}

		colorizeQuad(quad, this.inputContext);
		TerrainFormat.TERRAIN_ENCODER.encode(quad, inputContext, encodingContext, collectors.get((CanvasRenderMaterial) quad.material()));
	}
}
