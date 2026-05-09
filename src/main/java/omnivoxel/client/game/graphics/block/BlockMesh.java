package omnivoxel.client.game.graphics.block;

import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.common.BlockShape;
import omnivoxel.common.block.hitbox.BlockHitbox;
import omnivoxel.common.face.BlockFace;

public abstract class BlockMesh {
    protected String state;

    protected BlockMesh(String state) {
        this.state = state;
    }

    protected BlockMesh() {
        this(null);
    }

    public abstract String getID();

    public abstract String getModID();

    public abstract BlockShape getShape(BlockMesh top, BlockMesh bottom, BlockMesh north, BlockMesh south, BlockMesh east, BlockMesh west);

    public abstract BlockHitbox[] getHitbox();

    public abstract int[] getUVCoordinates(BlockFace blockFace);

    public abstract byte getLightDiffuse(LightChannels channel);

    public abstract byte getLightEmitting(LightChannels channel);

    public abstract boolean isTransparent();

    public abstract boolean shouldRenderTransparentMesh();

    public abstract boolean shouldRenderFace(BlockFace face, BlockMesh adjacentBlockMesh);

    public String getState() {
        return state;
    }
}