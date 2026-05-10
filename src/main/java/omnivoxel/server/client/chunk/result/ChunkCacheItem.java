package omnivoxel.server.client.chunk.result;

import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

public record ChunkCacheItem(Position3D chunkPosition, Chunk<ServerBlock> chunk) {
}