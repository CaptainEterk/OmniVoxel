package omnivoxel.client.game.graphics.api.opengl.mesh.definition;

import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.EntityMeshData;

public record GeneralEntityMeshDefinition(int solidVAO, int solidVBO, int solidEBO, int solidIndexCount,
                                          EntityMeshData meshData) implements EntityMeshDataDefinition {
}