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

package grondag.canvas.varia;

import com.mojang.math.Vector3f;

import net.minecraft.client.multiplayer.ClientLevel;

@FunctionalInterface
public interface CelestialObjectFunction {
	void compute(CelestialObjectInput input, CelestialObjectOutput output);

	CelestialObjectFunction VANILLA_SUN = (input, output) -> {
		final float angle = input.world().getTimeOfDay(input.tickDelta());
		output.hourAngle = angle * 360.0F;

		// FIX: This results in an abrupt transition around 740 time when the function returns null
		// Needs to be smoothed.

		// TODO: The color in the overworld is ugly.  Need to provide a way to override it
		// in celestial object JSON loading.

		final float[] fs = input.world().effects().getSunriseColor(angle, input.tickDelta());

		if (fs == null) {
			output.atmosphericColorModifier.set(1, 1, 1);
		} else {
			output.atmosphericColorModifier.set(fs[0], fs[1], fs[2]);
		}

		output.lightColor.set(1, 1, 1);
		output.illuminance = 32000f;
	};

	CelestialObjectFunction VANILLA_MOON = (input, output) -> {
		final float angle = input.world().getTimeOfDay(input.tickDelta());
		output.hourAngle = angle * 360.0F + 180F;

		output.atmosphericColorModifier.set(1, 1, 1);
		// based on vanilla sky lightmap at midnight
		// real moonlight is reddish but not so much
		output.lightColor.set(1, 0.5475f, 0.5475f);
		output.illuminance = 2000;
	};

	// Vanilla skylight 51%:
	// 0: dac7c7
	// noon: fbfbfb
	// 1000 (day): fbfbfb
	// 12500: bd9a9a
	// 13000 (night): 906262
	// 14000: 7e5151
	// 18000 (midnight): 7e5151

	// full brightness
	// noon: fcfcfc
	// midnight: b37676

	public interface CelestialObjectInput {
		ClientLevel world();
		float tickDelta();
		double cameraX();
		double cameraY();
		double cameraZ();
	}

	class CelestialObjectOutput {
		public float zenithAngle = 0;
		public float hourAngle = 0;

		public final Vector3f lightColor = new Vector3f();

		public final Vector3f atmosphericColorModifier = new Vector3f();

		public float illuminance;
	}
}
