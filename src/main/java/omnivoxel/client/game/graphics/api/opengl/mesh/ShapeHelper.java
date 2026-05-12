package omnivoxel.client.game.graphics.api.opengl.mesh;

import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.settings.ConstantCommonSettings;

public class ShapeHelper {
    private static final float MAX_PACKED_VALUE = 512f;
    private static final int BITMASK_3 = 0x7;
    private static final int BITMASK_8 = 0xFF;
    private static final int BITMASK_6 = 0x3F;
    private static final int BITMASK_13 = 0x1FFF;

    public static int[] packVertexData(Vertex vertex, int r, int g, int b, int s, BlockFace blockFace, int u, int v, int type) {
        int ix = (int) (vertex.px() * (MAX_PACKED_VALUE / ConstantCommonSettings.CHUNK_WIDTH));
        int iy = (int) (vertex.py() * (MAX_PACKED_VALUE / ConstantCommonSettings.CHUNK_HEIGHT));
        int iz = (int) (vertex.pz() * (MAX_PACKED_VALUE / ConstantCommonSettings.CHUNK_LENGTH));

        int packedPosition = (ix << 22) | (iy << 12) | (iz << 2);

        int normalPacked = blockFace.ordinal() & BITMASK_3;

        int uPacked = u & BITMASK_8;
        int vPacked = v & BITMASK_8;

        int packedType = type & BITMASK_13;

        int packedNormalTextureType = (normalPacked << 29)
                | (uPacked << 21)
                | (vPacked << 13)
                | (packedType);

        int rPacked = r & BITMASK_6;
        int gPacked = g & BITMASK_6;
        int bPacked = b & BITMASK_6;
        int sPacked = s & BITMASK_6;

        int packedLighting = (rPacked << 28)
                | (gPacked << 24)
                | (bPacked << 20)
                | (sPacked << 16);

        return new int[]{packedPosition, packedNormalTextureType, packedLighting};
    }
}