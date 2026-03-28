package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import omnivoxel.client.game.graphics.api.opengl.mesh.block.BlockMesh;
import omnivoxel.world.chunk.Chunk;

public record ChunkBlockData(Chunk<omnivoxel.world.block.Block> chunk, BlockMesh[] blockMeshes) {
}