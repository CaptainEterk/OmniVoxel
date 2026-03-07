package omnivoxel.client.game.graphics.opengl.mesh;

import omnivoxel.client.game.graphics.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.EntityMeshData;

import java.util.ArrayList;
import java.util.List;

public final class EntityMesh implements Mesh {
    private final EntityMeshDataDefinition definition;
    private final List<EntityMesh> children;
    private final EntityMeshData entityMeshData;

    public EntityMesh(
            EntityMeshDataDefinition definition, EntityMeshData entityMeshData
    ) {
        this.definition = definition;
        this.entityMeshData = entityMeshData;
        children = new ArrayList<>();
    }

    public void addChild(EntityMesh child) {
        children.add(child);
    }

    public List<EntityMesh> getChildren() {
        return children;
    }

    public EntityMeshDataDefinition getDefinition() {
        return definition;
    }

    public EntityMeshData getMeshData() {
        return entityMeshData;
    }

    @Override
    public String toString() {
        return "EntityMesh{" +
                "definition=" + definition +
                ", children=" + children +
                ", entityMeshData=" + entityMeshData +
                '}';
    }
}