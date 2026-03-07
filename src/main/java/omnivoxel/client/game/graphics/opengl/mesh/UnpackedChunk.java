package omnivoxel.client.game.graphics.opengl.mesh;

import omnivoxel.client.game.graphics.opengl.mesh.block.Block;

public record UnpackedChunk(Block[] blocks, boolean allSolid) {
}