package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;

public class ChunkShell<B> implements Chunk<B> {

    private static final int WIDTH = ConstantGameSettings.CHUNK_WIDTH;
    private static final int HEIGHT = ConstantGameSettings.CHUNK_HEIGHT;
    private static final int LENGTH = ConstantGameSettings.CHUNK_LENGTH;

    private final Object[] minX = new Object[HEIGHT * LENGTH];
    private final Object[] maxX = new Object[HEIGHT * LENGTH];

    private final Object[] minY = new Object[WIDTH * LENGTH];
    private final Object[] maxY = new Object[WIDTH * LENGTH];

    private final Object[] minZ = new Object[WIDTH * HEIGHT];
    private final Object[] maxZ = new Object[WIDTH * HEIGHT];

    @SuppressWarnings("unchecked")
    @Override
    public B getBlock(int x, int y, int z) {

        if (x == 0) {
            return (B) minX[y * LENGTH + z];
        }

        if (x == WIDTH - 1) {
            return (B) maxX[y * LENGTH + z];
        }

        if (y == 0) {
            return (B) minY[x * LENGTH + z];
        }

        if (y == HEIGHT - 1) {
            return (B) maxY[x * LENGTH + z];
        }

        if (z == 0) {
            return (B) minZ[x * HEIGHT + y];
        }

        if (z == LENGTH - 1) {
            return (B) maxZ[x * HEIGHT + y];
        }

        throw new IllegalArgumentException("Attempted to access interior block of ChunkShell");
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {

        if (x == 0) {
            minX[y * LENGTH + z] = block;
            return this;
        }

        if (x == WIDTH - 1) {
            maxX[y * LENGTH + z] = block;
            return this;
        }

        if (y == 0) {
            minY[x * LENGTH + z] = block;
            return this;
        }

        if (y == HEIGHT - 1) {
            maxY[x * LENGTH + z] = block;
            return this;
        }

        if (z == 0) {
            minZ[x * HEIGHT + y] = block;
            return this;
        }

        if (z == LENGTH - 1) {
            maxZ[x * HEIGHT + y] = block;
            return this;
        }

        throw new UnsupportedOperationException("Cannot set interior block in ChunkShell");
    }

    public void merge(ChunkShell<B> newShell) {
        for (int i = 0; i < minX.length; i++) {
            if (newShell.minX[i] != null) {
                minX[i] = newShell.minX[i];
            }
            if (newShell.maxX[i] != null) {
                maxX[i] = newShell.maxX[i];
            }
        }

        for (int i = 0; i < minY.length; i++) {
            if (newShell.minY[i] != null) {
                minY[i] = newShell.minY[i];
            }
            if (newShell.maxY[i] != null) {
                maxY[i] = newShell.maxY[i];
            }
        }

        for (int i = 0; i < minZ.length; i++) {
            if (newShell.minZ[i] != null) {
                minZ[i] = newShell.minZ[i];
            }
            if (newShell.maxZ[i] != null) {
                maxZ[i] = newShell.maxZ[i];
            }
        }
    }
}