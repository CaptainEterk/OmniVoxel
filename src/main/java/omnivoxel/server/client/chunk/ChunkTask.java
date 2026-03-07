package omnivoxel.server.client.chunk;

import omnivoxel.server.client.ServerClient;

public record ChunkTask(ServerClient serverClient, int x, int y, int z) {
}