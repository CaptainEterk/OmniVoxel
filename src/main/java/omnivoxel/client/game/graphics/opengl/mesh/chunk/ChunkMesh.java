package omnivoxel.client.game.graphics.opengl.mesh.chunk;

import omnivoxel.client.game.graphics.opengl.mesh.Mesh;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;

public interface ChunkMesh extends Mesh {
    int solidVAO();

    int solidVBO();

    int solidEBO();

    int solidIndexCount();

    int transparentVAO();

    int transparentVBO();

    int transparentEBO();

    int transparentIndexCount();

    MeshData meshData();
}