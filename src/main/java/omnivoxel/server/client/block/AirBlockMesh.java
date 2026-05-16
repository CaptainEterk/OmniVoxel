package omnivoxel.server.client.block;

import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.common.BlockShape;
import omnivoxel.common.block.hitbox.BlockHitbox;
import omnivoxel.common.face.BlockFace;

public final class AirBlockMesh extends BlockMesh {
    private final static int[][] emptyUVCoords = new int[6][0];
    private final static BlockHitbox[] emptyHitboxes = BlockHitbox.EMPTY_BLOCK_HITBOX;

    @Override
    public String getID() {
        return "air";
    }

    @Override
    public String getModID() {
        return "omnivoxel:" + getID();
    }

    @Override
    public BlockShape getShape(BlockMesh top, BlockMesh bottom, BlockMesh north, BlockMesh south, BlockMesh east, BlockMesh west) {
        return BlockShape.EMPTY_BLOCK_SHAPE;
    }

    @Override
    public BlockHitbox[] getHitbox() {
        return emptyHitboxes;
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
    public byte getLightDiffuse(LightChannels channel) {
        return 1;
    }

    @Override
    public byte getLightEmitting(LightChannels channel) {
        return 0;
    }

    @Override
    public boolean shouldRenderFace(BlockFace face, BlockMesh adjacentBlockMesh) {
        return false;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public boolean shouldRenderTransparentMesh() {
        return false;
    }
}