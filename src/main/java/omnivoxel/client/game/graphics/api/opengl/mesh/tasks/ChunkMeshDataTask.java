package omnivoxel.client.game.graphics.api.opengl.mesh.tasks;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.util.math.Position3D;

public record ChunkMeshDataTask(ByteBuf blocks, Position3D position3D) implements MeshDataTask {
}