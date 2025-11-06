package omnivoxel.server.client.block;

import omnivoxel.client.game.graphics.opengl.mesh.block.Block;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;

public final class AirBlock extends Block {
    private final static int[][] emptyUVCoords = new int[6][0];

    @Override
    public String getID() {
        return "air";
    }

    @Override
    public String getModID() {
        return "omnivoxel:" + getID();
    }

    @Override
    public BlockShape getShape(Block top, Block bottom, Block north, Block south, Block east, Block west) {
        return BlockShape.EMPTY_BLOCK_SHAPE;
    }

    @Override
    public String getState() {
        return "";
    }

    @Override
    public int[] getUVCoordinates(BlockFace blockFace) {
        return emptyUVCoords[blockFace.ordinal()];
    }

    @Override
    public boolean shouldRenderFace(BlockFace face, Block adjacentBlock) {
        return false;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }
}