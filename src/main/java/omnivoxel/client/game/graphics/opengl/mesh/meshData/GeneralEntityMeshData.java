package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import omnivoxel.client.game.entity.ClientEntity;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GeneralEntityMeshData implements EntityMeshData {
    private final ByteBuffer solidVertices;
    private final ByteBuffer solidIndices;
    private final ClientEntity entity;
    private final List<EntityMeshData> children;
    private Matrix4f model = new Matrix4f();

    public GeneralEntityMeshData(
            ByteBuffer solidVertices,
            ByteBuffer solidIndices,
            ClientEntity entity
    ) {
        this.solidVertices = solidVertices;
        this.solidIndices = solidIndices;
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
    public ByteBuffer transparentVertices() {
        return null;
    }

    @Override
    public ByteBuffer transparentIndices() {
        return null;
    }

    @Override
    public void cleanup() {
        // Free buffer data
        MemoryUtil.memFree(solidVertices);
        MemoryUtil.memFree(solidIndices);
    }

    @Override
    public void addChild(EntityMeshData entityMeshData) {
        children.add(entityMeshData);
    }

    @Override
    public ByteBuffer solidVertices() {
        return solidVertices;
    }

    @Override
    public ByteBuffer solidIndices() {
        return solidIndices;
    }

    @Override
    public ClientEntity entity() {
        return entity;
    }

    @Override
    public List<EntityMeshData> children() {
        return children;
    }
}