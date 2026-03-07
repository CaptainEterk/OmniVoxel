package omnivoxel.util;

import omnivoxel.client.game.settings.ConstantGameSettings;

public class IndexCalculator {
    public static int calculateBlockIndex(int x, int y, int z) {
        return x * ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_LENGTH + z * ConstantGameSettings.CHUNK_LENGTH + y;
    }

    public static int calculateBlockIndexPadded(int x, int y, int z) {
        return (x + 1) * ConstantGameSettings.PADDED_WIDTH * ConstantGameSettings.PADDED_HEIGHT + (z + 1) * ConstantGameSettings.PADDED_LENGTH + (y + 1);
    }

    public static int calculateBlockIndexPadded2D(int x, int z) {
        return (x + 1) * ConstantGameSettings.PADDED_WIDTH + z + 1;
    }
}