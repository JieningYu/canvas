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

package grondag.canvas.shader;

import net.minecraft.client.Minecraft;

import io.vram.sc.unordered.SimpleUnorderedArrayList;

import grondag.canvas.CanvasMod;
import grondag.canvas.config.Configurator;

public class GlProgramManager {
	public static final GlProgramManager INSTANCE = new GlProgramManager();

	private GlProgramManager() {
		if (Configurator.enableLifeCycleDebug) {
			CanvasMod.LOG.info("Lifecycle Event: UniformTicker init");
		}
	}

	private final SimpleUnorderedArrayList<GlProgram> programs = new SimpleUnorderedArrayList<>();

	public void onEndTick(Minecraft client) {
		final int limit = programs.size();

		for (int i = 0; i < limit; i++) {
			programs.get(i).onGameTick();
		}
	}

	public void onRenderTick() {
		final int limit = programs.size();

		for (int i = 0; i < limit; i++) {
			programs.get(i).onRenderTick();
		}
	}

	public void add(GlProgram program) {
		programs.addIfNotPresent(program);
	}

	public void remove(GlProgram program) {
		programs.remove(program);
	}

	public void reload() {
		programs.forEach(s -> s.forceReload());
		programs.clear();
	}
}
