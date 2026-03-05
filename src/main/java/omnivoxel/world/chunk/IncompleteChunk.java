package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * A lightweight chunk representation that stores only the 1-block-thick outer faces.
 * Intended for use as padding chunks around the main active chunk.
 */
public class IncompleteChunk<B> implements Chunk<B> {
    private static final int WIDTH = ConstantGameSettings.CHUNK_WIDTH;
    private static final int HEIGHT = ConstantGameSettings.CHUNK_HEIGHT;
    private static final int LENGTH = ConstantGameSettings.CHUNK_LENGTH;

    private final Map<Long, B> faceBlocks = new HashMap<>();
    private final B defaultBlock;

    public IncompleteChunk(B defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    @Override
    public B getBlock(int x, int y, int z) {
        if (!isOnFace(x, y, z)) {
            throw new RuntimeException("Attempted to access interior block of IncompleteChunk");
        }
        return faceBlocks.getOrDefault(encode(x, y, z), defaultBlock);
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {
        if (!isOnFace(x, y, z)) {
            return this;
        }
        faceBlocks.put(encode(x, y, z), block);
        return this;
    }

    private boolean isOnFace(int x, int y, int z) {
        return x == 0 || y == 0 || z == 0 ||
                x == WIDTH - 1 || y == HEIGHT - 1 || z == LENGTH - 1;
    }

    private long encode(int x, int y, int z) {
        // Efficient unique key for (x,y,z)
        return (((long) x & 0xFFFFFL) << 40)
                | (((long) y & 0xFFFFFL) << 20)
                | ((long) z & 0xFFFFFL);
    }

    // Optional: expose the faces map (for debugging)
    public Map<Long, B> getFaceBlocks() {
        return faceBlocks;
    }
}