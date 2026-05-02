package omnivoxel.client.game.graphics.block;

import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;

public class BlockMeshStateWrapper extends BlockMesh {
    private final BlockMesh wrappedBlockMesh;

    public BlockMeshStateWrapper(BlockMesh wrappedBlockMesh, String state) {
        super(state);
        wrappedBlockMesh.state = state;
        this.wrappedBlockMesh = wrappedBlockMesh;
    }

    @Override
    public String getID() {
        return wrappedBlockMesh.getID();
    }

    @Override
    public String getModID() {
        return wrappedBlockMesh.getModID();
    }

    @Override
    public BlockShape getShape(BlockMesh top, BlockMesh bottom, BlockMesh north, BlockMesh south, BlockMesh east, BlockMesh west) {
        return wrappedBlockMesh.getShape(top, bottom, north, south, east, west);
    }

    @Override
    public int[] getUVCoordinates(BlockFace blockFace) {
        return wrappedBlockMesh.getUVCoordinates(blockFace);
    }

    @Override
    public byte getLightDiffuse(LightChannels channel) {
        return wrappedBlockMesh.getLightDiffuse(channel);
    }

    @Override
    public byte getLightEmitting(LightChannels channel) {
        return wrappedBlockMesh.getLightEmitting(channel);
    }

    @Override
    public String getState() {
        return wrappedBlockMesh.state;
    }

    @Override
    public boolean isTransparent() {
        return wrappedBlockMesh.isTransparent();
    }

    @Override
    public boolean shouldRenderTransparentMesh() {
        return wrappedBlockMesh.shouldRenderTransparentMesh();
    }

    @Override
    public boolean shouldRenderFace(BlockFace face, BlockMesh adjacentBlockMesh) {
        return wrappedBlockMesh.shouldRenderFace(face, adjacentBlockMesh);
    }
}
