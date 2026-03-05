package omnivoxel.client.game.graphics.opengl.mesh.definition;

import omnivoxel.client.game.graphics.opengl.mesh.meshData.EntityMeshData;

public record EntityMeshDataNoDefinition(EntityMeshData meshData) implements EntityMeshDataDefinition {
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