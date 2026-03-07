package omnivoxel.client.game.graphics.opengl.mesh.definition;

import omnivoxel.client.game.graphics.opengl.mesh.meshData.EntityMeshData;

public interface EntityMeshDataDefinition {
    int solidVAO();

    int solidVBO();

    int solidEBO();

    int solidIndexCount();

    EntityMeshData meshData();
}