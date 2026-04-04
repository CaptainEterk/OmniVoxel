package omnivoxel.server.client.chunk;

import omnivoxel.server.client.ServerClient;
import omnivoxel.util.thread.worker.WorkerTask;

public record ChunkTask(ServerClient serverClient, int x, int y, int z) implements WorkerTask {
    @Override
    public void reject() {

    }
}