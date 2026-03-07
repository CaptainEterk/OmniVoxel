package omnivoxel.client.game.graphics.opengl.mesh;

import omnivoxel.client.game.graphics.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.common.face.BlockFace;

public class ShapeHelper {
    public static final float PIXEL = 0.0625f;         // Precision of 0.0625 (1/16)
    private static final float MAX_PACKED_VALUE = 512f;      // Maximum value for 10-bit (2^10 - 1)

    private static final int BITMASK_5 = 0x1F;  // 5-bit mask
    private static final int BITMASK_4 = 0xF;  // 4-bit mask
    private static final int BITMASK_3 = 0x7;  // 3-bit mask
    private static final int BITMASK_8 = 0xFF; // 8-bit mask
    private static final int BITMASK_10 = 1023; // 8-bit mask

    public static int[] packVertexData(Vertex vertex, int ao, float r, float g, float b, BlockFace blockFace, int u, int v, int type) {
        // Position calculations

        int ix = (int) (vertex.px() * (MAX_PACKED_VALUE / ConstantGameSettings.CHUNK_WIDTH));
        int iy = (int) (vertex.py() * (MAX_PACKED_VALUE / ConstantGameSettings.CHUNK_HEIGHT));
        int iz = (int) (vertex.pz() * (MAX_PACKED_VALUE / ConstantGameSettings.CHUNK_LENGTH));

        // Pack the position data into a 32-bit integer
        int packedPosition = (ix << 22) | (iy << 12) | (iz << 2) | ao;

        // Normalize and pack color (4 bits each)
        int rPacked = ((int) (r * BITMASK_4)) & BITMASK_4;
        int gPacked = ((int) (g * BITMASK_4)) & BITMASK_4;
        int bPacked = ((int) (b * BITMASK_4)) & BITMASK_4;

        // Pack normal (3 bits)
        int normalPacked = blockFace.ordinal() & BITMASK_3;

        // Pack UV coordinates (8 bits each)
        int uPacked = u & BITMASK_8;
        int vPacked = v & BITMASK_8;

        // Pack color, normal, and UV into a second 32-bit integer
        int packedColorNormalUV = (rPacked << 28)  // R in top 4 bits
                | (gPacked << 24)  // G in next 4 bits
                | (bPacked << 20)  // B in next 4 bits
                | (normalPacked << 17)  // Normal in next 3 bits
                | (uPacked << 9)  // U in next 8 bits
                | (vPacked << 1);  // V in lowest 8 bits

        return new int[]{packedPosition, packedColorNormalUV, type};
    }
}