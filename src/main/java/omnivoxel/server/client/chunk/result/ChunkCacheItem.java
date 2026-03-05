package omnivoxel.server.client.chunk.result;

import omnivoxel.util.math.Position3D;

public record ChunkCacheItem(Position3D chunkPosition, byte[] bytes) {
}