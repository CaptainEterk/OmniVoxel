package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import omnivoxel.client.game.entity.ClientEntity;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ModelEntityMeshData implements EntityMeshData {
    private final ClientEntity entity;
    private final List<EntityMeshData> children;
    private Matrix4f model = new Matrix4f();

    public ModelEntityMeshData(ClientEntity entity) {
        this.entity = entity;
        this.children = new ArrayList<>();
    }

    @Override
    public Matrix4f getModel() {
        return model;
    }

    @Override
    public EntityMeshData setModel(Matrix4f model) {
        this.model = model;
        return this;
    }

    @Override
    public void addChild(EntityMeshData entityMeshData) {
        children.add(entityMeshData);
    }

    @Override
    public ClientEntity entity() {
        return entity;
    }

    @Override
    public List<EntityMeshData> children() {
        return children;
    }

    @Override
    public ByteBuffer solidVertices() {
        return null;
    }

    @Override
    public ByteBuffer solidIndices() {
        return null;
    }

    @Override
    public ByteBuffer transparentVertices() {
        return null;
    }

    @Override
    public ByteBuffer transparentIndices() {
        return null;
    }

    @Override
    public void cleanup() {
    }
}