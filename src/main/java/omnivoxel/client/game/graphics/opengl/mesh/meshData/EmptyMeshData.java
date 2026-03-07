package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import java.nio.ByteBuffer;

public class EmptyMeshData implements MeshData {
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