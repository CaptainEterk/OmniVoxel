package omnivoxel.client.game.graphics.api.opengl.mesh.tasks;

import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.util.math.Position3D;

import java.util.Objects;

public record ChunkMeshDataTask(Position3D position3D) implements MeshDataTask {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChunkMeshDataTask that = (ChunkMeshDataTask) o;
        return Objects.equals(position3D, that.position3D);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(position3D);
    }

    @Override
    public void reject() {
    }
}