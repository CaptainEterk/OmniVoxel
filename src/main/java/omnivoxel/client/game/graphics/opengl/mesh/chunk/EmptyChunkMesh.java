package omnivoxel.client.game.graphics.opengl.mesh.chunk;

import omnivoxel.client.game.graphics.opengl.mesh.meshData.EmptyMeshData;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;

public class EmptyChunkMesh implements ChunkMesh {
    private static final MeshData meshData = new EmptyMeshData();

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

    @Override
    public int transparentVAO() {
        return 0;
    }

    @Override
    public int transparentVBO() {
        return 0;
    }

    @Override
    public int transparentEBO() {
        return 0;
    }

    @Override
    public int transparentIndexCount() {
        return 0;
    }

    @Override
    public MeshData meshData() {
        return meshData;
    }
}
