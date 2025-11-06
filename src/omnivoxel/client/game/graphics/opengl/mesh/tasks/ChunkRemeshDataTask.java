package omnivoxel.client.game.graphics.opengl.mesh.tasks;

import omnivoxel.client.game.graphics.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.generators.ChunkBlockData;
import omnivoxel.util.math.Position3D;

public record ChunkRemeshDataTask(ChunkBlockData chunk, Position3D position3D) implements MeshDataTask {
    @Override
    public void cleanup() {
    }
}