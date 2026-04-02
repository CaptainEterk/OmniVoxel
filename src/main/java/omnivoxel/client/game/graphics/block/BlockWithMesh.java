package omnivoxel.client.game.graphics.block;

import omnivoxel.world.block.Block;

public class BlockWithMesh extends Block {
    private final BlockMesh blockMesh;

    public BlockWithMesh(String id, BlockMesh blockMesh) {
        super(id);
        this.blockMesh = blockMesh;
    }

    public BlockMesh blockMesh() {
        return blockMesh;
    }
}