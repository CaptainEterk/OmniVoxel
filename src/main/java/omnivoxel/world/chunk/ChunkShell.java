package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;

// TODO: Memory optimizations
public class ChunkShell<B> implements Chunk<B> {
    private final Object[] minX = new Object[ConstantGameSettings.CHUNK_HEIGHT * ConstantGameSettings.CHUNK_LENGTH];
    private final Object[] maxX = new Object[ConstantGameSettings.CHUNK_HEIGHT * ConstantGameSettings.CHUNK_LENGTH];

    private final Object[] minY = new Object[ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_LENGTH];
    private final Object[] maxY = new Object[ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_LENGTH];

    private final Object[] minZ = new Object[ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_HEIGHT];
    private final Object[] maxZ = new Object[ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_HEIGHT];

    // May return null if one of the sides has not been initialized
    @SuppressWarnings("unchecked")
    @Override
    public B getBlock(int x, int y, int z) {
        if (x == 0) {
            return (B) minX[y * ConstantGameSettings.CHUNK_LENGTH + z];
        }

        if (x == ConstantGameSettings.CHUNK_WIDTH - 1) {
            return (B) maxX[y * ConstantGameSettings.CHUNK_LENGTH + z];
        }

        if (y == 0) {
            return (B) minY[x * ConstantGameSettings.CHUNK_LENGTH + z];
        }

        if (y == ConstantGameSettings.CHUNK_HEIGHT - 1) {
            return (B) maxY[x * ConstantGameSettings.CHUNK_LENGTH + z];
        }

        if (z == 0) {
            return (B) minZ[x * ConstantGameSettings.CHUNK_HEIGHT + y];
        }

        if (z == ConstantGameSettings.CHUNK_LENGTH - 1) {
            return (B) maxZ[x * ConstantGameSettings.CHUNK_HEIGHT + y];
        }

        throw new IllegalArgumentException("Attempted to access interior block of ChunkShell");
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {

        if (x == 0) {
            minX[y * ConstantGameSettings.CHUNK_LENGTH + z] = block;
            return this;
        }

        if (x == ConstantGameSettings.CHUNK_WIDTH - 1) {
            maxX[y * ConstantGameSettings.CHUNK_LENGTH + z] = block;
            return this;
        }

        if (y == 0) {
            minY[x * ConstantGameSettings.CHUNK_LENGTH + z] = block;
            return this;
        }

        if (y == ConstantGameSettings.CHUNK_HEIGHT - 1) {
            maxY[x * ConstantGameSettings.CHUNK_LENGTH + z] = block;
            return this;
        }

        if (z == 0) {
            minZ[x * ConstantGameSettings.CHUNK_HEIGHT + y] = block;
            return this;
        }

        if (z == ConstantGameSettings.CHUNK_LENGTH - 1) {
            maxZ[x * ConstantGameSettings.CHUNK_HEIGHT + y] = block;
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