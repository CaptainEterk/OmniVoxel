package omnivoxel.client.game.graphics.opengl.mesh.chunk;

import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;

public record TransparentChunkMesh(
        int transparentVAO, int transparentVBO, int transparentEBO, int transparentIndexCount,
        MeshData meshData
) implements ChunkMesh {
    @Override
    public int solidVAO() {
        return 0;
    }

    @Override
    public int solidVBO() {
        return 0;
    }

    @Override
    public int solidEBO() {
        return 0;
    }

    @Override
    public int solidIndexCount() {
        return 0;
    }
}