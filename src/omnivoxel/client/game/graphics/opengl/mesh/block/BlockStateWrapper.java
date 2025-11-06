package omnivoxel.client.game.graphics.opengl.mesh.block;

import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;

public class BlockStateWrapper extends Block {
    private final Block wrappedBlock;

    public BlockStateWrapper(Block wrappedBlock, String state) {
        super(state);
        wrappedBlock.state = state;
        this.wrappedBlock = wrappedBlock;
    }

    @Override
    public String getID() {
        return wrappedBlock.getID();
    }

    @Override
    public String getModID() {
        return wrappedBlock.getModID();
    }

    @Override
    public BlockShape getShape(Block top, Block bottom, Block north, Block south, Block east, Block west) {
        return wrappedBlock.getShape(top, bottom, north, south, east, west);
    }

    @Override
    public int[] getUVCoordinates(BlockFace blockFace) {
        return wrappedBlock.getUVCoordinates(blockFace);
    }

    @Override
    public String getState() {
        return wrappedBlock.state;
    }

    @Override
    public boolean isTransparent() {
        return wrappedBlock.isTransparent();
    }

    @Override
    public boolean shouldRenderTransparentMesh() {
        return wrappedBlock.shouldRenderTransparentMesh();
    }

    @Override
    public boolean shouldRenderFace(BlockFace face, Block adjacentBlock) {
        return wrappedBlock.shouldRenderFace(face, adjacentBlock);
    }
}
