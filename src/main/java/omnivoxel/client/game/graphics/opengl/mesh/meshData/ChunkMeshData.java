package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import omnivoxel.util.math.Position3D;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public record ChunkMeshData(
        ByteBuffer solidVertices,
        ByteBuffer solidIndices,
        ByteBuffer transparentVertices,
        ByteBuffer transparentIndices,
        Position3D chunkPosition
) implements MeshData {
    @Override
    public void cleanup() {
        // Free buffer data
        MemoryUtil.memFree(solidVertices);
        MemoryUtil.memFree(solidIndices);
        MemoryUtil.memFree(transparentVertices);
        MemoryUtil.memFree(transparentIndices);
    }
}