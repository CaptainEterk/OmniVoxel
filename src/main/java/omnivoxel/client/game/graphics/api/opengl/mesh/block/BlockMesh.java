package omnivoxel.client.game.graphics.api.opengl.mesh.block;

import omnivoxel.common.BlockShape;
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

    public abstract int[] getUVCoordinates(BlockFace blockFace);

    public boolean isTransparent() {
        return false;
    }

    public boolean shouldRenderTransparentMesh() {
        return false;
    }

    public boolean shouldRenderFace(BlockFace face, BlockMesh adjacentBlockMesh) {
        return adjacentBlockMesh.isTransparent();
    }

    public String getState() {
        return state;
    }
}