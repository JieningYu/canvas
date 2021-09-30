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

package grondag.canvas.buffer.render;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.jetbrains.annotations.Nullable;

public interface TransferBuffer {
	int sizeBytes();

	void put(int[] source, int sourceStart, int targetStart, int length);

	ShortBuffer shortBuffer();

	ByteBuffer byteBuffer();

	/** MUST be called if one of other release methods isn't. ALWAYS returns null. */
	@Nullable
	TransferBuffer release();

	/** ALWAYS returns null. */
	@Nullable
	default TransferBuffer releaseToBoundBuffer(int target, int targetStartBytes) {
		transferToBoundBuffer(target, targetStartBytes, 0, sizeBytes());
		return release();
	}

	void transferToBoundBuffer(int target, int targetStartBytes, int sourceStartBytes, int lengthBytes);
}
