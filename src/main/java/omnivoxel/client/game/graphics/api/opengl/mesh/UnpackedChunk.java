package omnivoxel.client.game.graphics.api.opengl.mesh;

import omnivoxel.client.game.graphics.api.opengl.mesh.block.BlockMesh;

public record UnpackedChunk(BlockMesh[] blockMeshes, boolean allSolid) {
}