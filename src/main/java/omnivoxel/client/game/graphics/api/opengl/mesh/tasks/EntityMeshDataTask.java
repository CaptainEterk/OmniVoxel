package omnivoxel.client.game.graphics.api.opengl.mesh.tasks;

import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;

public record EntityMeshDataTask(ClientEntity entity) implements MeshDataTask {
}