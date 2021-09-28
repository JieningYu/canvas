/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;

import grondag.canvas.mixinterface.BufferBuilderExt;
import grondag.canvas.mixinterface.Matrix4fExt;

@Mixin(BakedGlyph.class)
public abstract class MixinBakedGlyph {
	@Shadow private float u0;
	@Shadow private float u1;
	@Shadow private float v0;
	@Shadow private float v1;
	@Shadow private float left;
	@Shadow private float right;
	@Shadow private float up;
	@Shadow private float down;

	private static final Vector3f pos = new Vector3f();
	// NB: size in bytes is size of integer array for whole quad
	private static final int[] quadData = new int[DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.getVertexSize()];

	// PERF: consider handling drawRectangle also

	/**
	 * @author grondag
	 * @reason performance; calls are too frequent and parameter stack too large for inject to perform well
	 */
	@Overwrite
	public void render(boolean oblique, float x, float y, Matrix4f matrix4f, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int lightmap) {
		final float x0 = x + left;
		final float x1 = x + right;
		final float y0 = up - 3.0F;
		final float y1 = down - 3.0F;
		final float top = y + y0;
		final float bottom = y + y1;
		final float obqTop = oblique ? 1.0F - 0.25F * y0 : 0.0F;
		final float obqBotom = oblique ? 1.0F - 0.25F * y1 : 0.0F;

		if (vertexConsumer instanceof final BufferBuilderExt extBuilder
				&& extBuilder.canvas_canSupportDirect(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP)
				&& RenderSystem.isOnRenderThread() // This last is because we are using static vars
		) {
			final Matrix4fExt matrix = Matrix4fExt.cast(matrix4f);
			final int color = (int) (red * 255.0F) | ((int) (green * 255.0F) << 8) | ((int) (blue * 255.0F) << 16) | ((int) (alpha * 255.0F) << 24);
			int i = 0;

			pos.set(x0 + obqTop, top, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u0);
			quadData[i++] = Float.floatToRawIntBits(v0);
			quadData[i++] = lightmap;

			pos.set(x0 + obqBotom, bottom, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u0);
			quadData[i++] = Float.floatToRawIntBits(v1);
			quadData[i++] = lightmap;

			pos.set(x1 + obqBotom, bottom, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u1);
			quadData[i++] = Float.floatToRawIntBits(v1);
			quadData[i++] = lightmap;

			pos.set(x1 + obqTop, top, 0.0F);
			matrix.fastTransform(pos);
			quadData[i++] = Float.floatToRawIntBits(pos.x());
			quadData[i++] = Float.floatToRawIntBits(pos.y());
			quadData[i++] = Float.floatToRawIntBits(pos.z());
			quadData[i++] = color;
			quadData[i++] = Float.floatToRawIntBits(u1);
			quadData[i++] = Float.floatToRawIntBits(v0);
			quadData[i++] = lightmap;

			assert i == quadData.length;
			extBuilder.canvas_putQuadDirect(quadData);
		} else {
			vertexConsumer.vertex(matrix4f, x0 + obqTop, top, 0.0F).color(red, green, blue, alpha).uv(u0, v0).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x0 + obqBotom, bottom, 0.0F).color(red, green, blue, alpha).uv(u0, v1).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x1 + obqBotom, bottom, 0.0F).color(red, green, blue, alpha).uv(u1, v1).uv2(lightmap).endVertex();
			vertexConsumer.vertex(matrix4f, x1 + obqTop, top, 0.0F).color(red, green, blue, alpha).uv(u1, v0).uv2(lightmap).endVertex();
		}
	}
}
