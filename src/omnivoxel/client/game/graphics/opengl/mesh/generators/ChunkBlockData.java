package omnivoxel.client.game.graphics.opengl.mesh.generators;

import omnivoxel.client.game.graphics.opengl.mesh.block.Block;
import omnivoxel.world.chunk.Chunk;

public record ChunkBlockData(Chunk<omnivoxel.world.block.Block> chunk, Block[] blocks) {
}