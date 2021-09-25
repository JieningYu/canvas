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

package grondag.canvas.pipeline;

import org.lwjgl.opengl.GL46;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import grondag.canvas.pipeline.config.ImageConfig;
import grondag.canvas.pipeline.config.util.NamedDependency;

public class ProgramTextureData {
	public final int[] texIds;
	public final int[] texTargets;

	public ProgramTextureData(NamedDependency<ImageConfig>[] samplerImages) {
		texIds = new int[samplerImages.length];
		texTargets = new int[samplerImages.length];

		for (int i = 0; i < samplerImages.length; ++i) {
			final String imageName = samplerImages[i].name;

			int imageBind = 0;
			int bindTarget = GL46.GL_TEXTURE_2D;

			if (imageName.contains(":")) {
				final AbstractTexture tex = tryLoadResourceTexture(new ResourceLocation(imageName));

				if (tex != null) {
					imageBind = tex.getId();
				}
			} else {
				final Image img = Pipeline.getImage(imageName);

				if (img != null) {
					imageBind = img.glId();
					bindTarget = img.config.target;
				}
			}

			texIds[i] = imageBind;
			texTargets[i] = bindTarget;
		}
	}

	private static AbstractTexture tryLoadResourceTexture(ResourceLocation identifier) {
		final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		final AbstractTexture existingTexture = textureManager.getTexture(identifier);

		if (existingTexture != null) {
			return existingTexture;
		} else {
			// NB: `registerTexture` will replace the texture with MissingSprite if not found. This is useful for
			//     pipeline developers.
			//     Additionally, TextureManager will handle removing missing textures on resource reload.
			final SimpleTexture resourceTexture = new SimpleTexture(identifier);
			textureManager.register(identifier, resourceTexture);
			return textureManager.getTexture(identifier);
		}
	}
}
