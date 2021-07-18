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

package grondag.canvas.render.terrain.cluster;

import com.mojang.blaze3d.systems.RenderSystem;

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.render.UploadableVertexStorage;
import grondag.canvas.render.terrain.TerrainFormat;

public class ClusteredDrawableStorage implements UploadableVertexStorage {
	private static final int NOT_ALLOCATED = -1;

	private final ClusteredVertexStorage owner;
	private TransferBuffer transferBuffer;
	final int byteCount;
	final int triVertexCount;
	private int baseVertex = NOT_ALLOCATED;
	private boolean isClosed = false;
	final long clumpPos;
	private ClusteredVertexStorageClump clump = null;
	int paddingBytes;

	public ClusteredDrawableStorage(ClusteredVertexStorage owner, TransferBuffer transferBuffer, int byteCount, long packedOriginBlockPos, int triVertexCount) {
		this.owner = owner;
		this.transferBuffer = transferBuffer;
		this.byteCount = byteCount;
		this.triVertexCount = triVertexCount;
		clumpPos = ClusteredVertexStorage.clumpPos(packedOriginBlockPos);
	}

	TransferBuffer getAndClearTransferBuffer() {
		TransferBuffer result = transferBuffer;
		transferBuffer = null;
		return result;
	}

	@Override
	public ClusteredDrawableStorage release() {
		close(true);

		return null;
	}

	public void close(boolean notify) {
		assert RenderSystem.isOnRenderThread();

		if (!isClosed) {
			isClosed = true;

			if (transferBuffer != null) {
				transferBuffer = transferBuffer.release();
			}

			if (clump != null) {
				if (notify) {
					clump.notifyClosed(this);
				}

				clump = null;
			}

			baseVertex = NOT_ALLOCATED;
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * Controlled by storage so that the vertices can be moved around as
	 * needed to control fragmentation without external entanglements.
	 */
	public int baseVertex() {
		assert baseVertex != NOT_ALLOCATED;

		return baseVertex;
	}

	public int baseByteAddress() {
		assert baseVertex != NOT_ALLOCATED;

		return baseVertex * TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes;
	}

	void setBaseAddress(int baseAddress) {
		baseVertex = baseAddress / TerrainFormat.TERRAIN_MATERIAL.vertexStrideBytes;
		//assert clump.isPresent(this);
	}

	void setClump(ClusteredVertexStorageClump clump) {
		assert baseVertex == NOT_ALLOCATED;
		assert this.clump == null;
		assert clump != null;
		this.clump = clump;
	}

	ClusteredVertexStorageClump getClump() {
		//assert clump.isPresent(this);

		return clump;
	}

	@Override
	public void upload() {
		assert baseVertex == NOT_ALLOCATED;
		owner.allocate(this);
	}
}
