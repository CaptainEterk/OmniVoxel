package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import omnivoxel.client.game.entity.ClientEntity;
import org.joml.Matrix4f;

import java.util.List;

public interface EntityMeshData extends MeshData {
    Matrix4f getModel();

    EntityMeshData setModel(Matrix4f model);

    void addChild(EntityMeshData entityMeshData);

    ClientEntity entity();

    List<EntityMeshData> children();
}