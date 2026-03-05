package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.util.IndexCalculator;

public class GeneralChunk<B> implements Chunk<B> {
    private final B[] blocks;

    @SuppressWarnings("unchecked")
    public GeneralChunk() {
        this.blocks = (B[]) new Object[ConstantGameSettings.BLOCKS_IN_CHUNK];
    }

    public GeneralChunk(Chunk<B> chunk) {
        this.blocks = extractBlocks(chunk);
    }

    @SuppressWarnings("unchecked")
    private B[] extractBlocks(Chunk<B> chunk) {
        B[] blocks = (B[]) new Object[ConstantGameSettings.BLOCKS_IN_CHUNK];
        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {
                    blocks[IndexCalculator.calculateBlockIndex(x, y, z)] = chunk.getBlock(x, y, z);
                }
            }
        }
        return blocks;
    }

    @Override
    public B getBlock(int x, int y, int z) {
        return blocks[IndexCalculator.calculateBlockIndex(x, y, z)];
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {
        blocks[IndexCalculator.calculateBlockIndex(x, y, z)] = block;
        return this;
    }
}