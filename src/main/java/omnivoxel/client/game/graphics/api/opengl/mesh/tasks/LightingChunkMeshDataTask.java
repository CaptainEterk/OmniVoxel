package omnivoxel.client.game.graphics.api.opengl.mesh.tasks;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.util.math.Position3D;

public record LightingChunkMeshDataTask(ByteBuf blocks, Position3D position3D, boolean overflow) implements MeshDataTask {
    public LightingChunkMeshDataTask(ByteBuf blocks, Position3D position3D) {
        this(blocks, position3D, false);
    }
}