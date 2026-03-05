package omnivoxel.client.game.graphics.opengl.mesh.chunk;

import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;

public record GeneralChunkMesh(
        int solidVAO, int solidVBO, int solidEBO, int solidIndexCount,
        int transparentVAO, int transparentVBO, int transparentEBO, int transparentIndexCount,
        MeshData meshData
) implements ChunkMesh {
}