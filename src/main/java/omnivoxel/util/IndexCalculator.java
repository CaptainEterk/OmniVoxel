package omnivoxel.util;

import omnivoxel.client.game.settings.ConstantGameSettings;

public class IndexCalculator {
    // Precompute these once
    private static final int Y_BITS = Integer.numberOfTrailingZeros(ConstantGameSettings.CHUNK_LENGTH);
    private static final int Z_BITS = Integer.numberOfTrailingZeros(ConstantGameSettings.CHUNK_WIDTH);
    private static final int SLICE_BITS = Y_BITS + Z_BITS;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int Z_MASK = (1 << Z_BITS) - 1;

    public static int calculateBlockIndex(int x, int y, int z) {
        return x * ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_LENGTH + z * ConstantGameSettings.CHUNK_LENGTH + y;
    }

    public static int calculateBlockIndexPadded(int x, int y, int z) {
        return (x + 1) * ConstantGameSettings.PADDED_WIDTH * ConstantGameSettings.PADDED_HEIGHT + (z + 1) * ConstantGameSettings.PADDED_LENGTH + (y + 1);
    }

    public static int calculateBlockIndex2D(int x, int z) {
        return x * ConstantGameSettings.CHUNK_WIDTH + z;
    }

    public static int calculateBlockIndexPadded2D(int x, int z) {
        return (x + 1) * ConstantGameSettings.PADDED_WIDTH + z + 1;
    }

    public static boolean checkBounds(int nx, int ny, int nz) {
        return nx >= 0 && nx < ConstantGameSettings.CHUNK_WIDTH && ny >= 0 && ny < ConstantGameSettings.CHUNK_HEIGHT && nz >= 0 && nz < ConstantGameSettings.CHUNK_LENGTH;
    }

    public static int x(int index) {
        return index >> SLICE_BITS;
    }

    public static int z(int index) {
        return (index >> Y_BITS) & Z_MASK;
    }

    public static int y(int index) {
        return index & Y_MASK;
    }
}