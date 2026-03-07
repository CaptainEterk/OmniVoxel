package omnivoxel.client.game.graphics.opengl.mesh.block;

import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;

public abstract class Block {
    protected String state;

    protected Block(String state) {
        this.state = state;
    }

    protected Block() {
        this(null);
    }

    public abstract String getID();

    public abstract String getModID();

    public abstract BlockShape getShape(Block top, Block bottom, Block north, Block south, Block east, Block west);

    public abstract int[] getUVCoordinates(BlockFace blockFace);

    public boolean isTransparent() {
        return false;
    }

    public boolean shouldRenderTransparentMesh() {
        return false;
    }

    public boolean shouldRenderFace(BlockFace face, Block adjacentBlock) {
        return adjacentBlock.isTransparent();
    }

    public String getState() {
        return state;
    }
}