package omnivoxel.server.client.chunk.result;

import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.world.chunk.Chunk;

public record ChunkResult(byte[] bytes, Chunk<ServerBlock> chunk) {
}