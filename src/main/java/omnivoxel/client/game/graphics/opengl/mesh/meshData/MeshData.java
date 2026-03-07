package omnivoxel.client.game.graphics.opengl.mesh.meshData;

import java.nio.ByteBuffer;

public interface MeshData {
    ByteBuffer solidVertices();

    ByteBuffer solidIndices();

    ByteBuffer transparentVertices();

    ByteBuffer transparentIndices();

    void cleanup();
}