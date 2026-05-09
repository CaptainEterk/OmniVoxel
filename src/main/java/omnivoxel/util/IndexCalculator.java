package omnivoxel.util;

import omnivoxel.common.settings.ConstantCommonSettings;

public class IndexCalculator {
    private static final int X_BITS = Integer.numberOfTrailingZeros(ConstantCommonSettings.CHUNK_WIDTH);

    private static final int Y_BITS = Integer.numberOfTrailingZeros(ConstantCommonSettings.CHUNK_HEIGHT);

    private static final int Z_BITS = Integer.numberOfTrailingZeros(ConstantCommonSettings.CHUNK_LENGTH);

    private static final int X_MASK = ConstantCommonSettings.CHUNK_WIDTH - 1;
    private static final int Y_MASK = ConstantCommonSettings.CHUNK_HEIGHT - 1;
    private static final int Z_MASK = ConstantCommonSettings.CHUNK_LENGTH - 1;

    private static final int Z_SHIFT = Y_BITS;
    private static final int X_SHIFT = Y_BITS + Z_BITS;

    public static int calculateBlockIndex(int x, int y, int z) {
        return (x << X_SHIFT)
                | (z << Z_SHIFT)
                | y;
    }

    public static int calculateBlockIndexPadded(int x, int y, int z) {
        return (x + 1) * ConstantCommonSettings.PADDED_WIDTH * ConstantCommonSettings.PADDED_HEIGHT
                + (z + 1) * ConstantCommonSettings.PADDED_LENGTH
                + (y + 1);
    }

    public static int calculateBlockIndex2D(int x, int z) {
        return x * ConstantCommonSettings.CHUNK_WIDTH + z;
    }

    public static int calculateBlockIndexPadded2D(int x, int z) {
        return (x + 1) * ConstantCommonSettings.PADDED_WIDTH + z + 1;
    }

    public static boolean checkBounds(int nx, int ny, int nz) {
        return nx >= 0 && nx < ConstantCommonSettings.CHUNK_WIDTH
                && ny >= 0 && ny < ConstantCommonSettings.CHUNK_HEIGHT
                && nz >= 0 && nz < ConstantCommonSettings.CHUNK_LENGTH;
    }

    public static int x(int index) {
        return index >> X_SHIFT;
    }

    public static int z(int index) {
        return (index >> Z_SHIFT) & Z_MASK;
    }

    public static int y(int index) {
        return index & Y_MASK;
    }

    public static int chunkX(int worldX) {
        return worldX >> X_BITS;
    }

    public static int chunkY(int worldY) {
        return worldY >> Y_BITS;
    }

    public static int chunkZ(int worldZ) {
        return worldZ >> Z_BITS;
    }

    public static int localX(int worldX) {
        return worldX & X_MASK;
    }

    public static int localY(int worldY) {
        return worldY & Y_MASK;
    }

    public static int localZ(int worldZ) {
        return worldZ & Z_MASK;
    }

    public static int worldToBlockIndex(int worldX, int worldY, int worldZ) {
        return calculateBlockIndex(
                localX(worldX),
                localY(worldY),
                localZ(worldZ)
        );
    }

    public static long packChunkPosition(int chunkX, int chunkY, int chunkZ) {
        return ((long) (chunkX & 0x1FFFFF) << 42)
                | ((long) (chunkY & 0x1FFFFF) << 21)
                | ((long) (chunkZ & 0x1FFFFF));
    }

    public static int unpackChunkX(long packed) {
        return signExtend21((int) (packed >> 42));
    }

    public static int unpackChunkY(long packed) {
        return signExtend21((int) (packed >> 21));
    }

    public static int unpackChunkZ(long packed) {
        return signExtend21((int) packed);
    }

    private static int signExtend21(int value) {
        value &= 0x1FFFFF;

        if ((value & 0x100000) != 0) {
            value |= ~0x1FFFFF;
        }

        return value;
    }
}