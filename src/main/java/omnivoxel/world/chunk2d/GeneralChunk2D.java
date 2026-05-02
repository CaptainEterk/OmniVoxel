package omnivoxel.world.chunk2d;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;

public class GeneralChunk2D<B> implements Chunk2D<B> {
    private final B[] blocks;

    @SuppressWarnings("unchecked")
    public GeneralChunk2D() {
        this.blocks = (B[]) new Object[ConstantCommonSettings.BLOCKS_IN_CHUNK_2D];
    }

    public GeneralChunk2D(Chunk2D<B> chunk) {
        this.blocks = extractBlocks(chunk);
    }

    @SuppressWarnings("unchecked")
    private B[] extractBlocks(Chunk2D<B> chunk) {
        B[] blocks = (B[]) new Object[ConstantCommonSettings.BLOCKS_IN_CHUNK_2D];
        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                blocks[IndexCalculator.calculateBlockIndex2D(x, z)] = chunk.getBlock(x, z);

            }
        }
        return blocks;
    }

    @Override
    public B getBlock(int x, int z) {
        return blocks[IndexCalculator.calculateBlockIndex2D(x, z)];
    }

    @Override
    public Chunk2D<B> setBlock(int x, int z, B block) {
        blocks[IndexCalculator.calculateBlockIndex2D(x, z)] = block;
        return this;
    }
}