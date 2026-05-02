package omnivoxel.world.chunk;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;

public class PaddedChunkResult<B> implements Chunk<B> {
    private final B[] blocks;

    @SuppressWarnings("unchecked")
    public PaddedChunkResult() {
        this.blocks = (B[]) new Object[ConstantCommonSettings.BLOCKS_IN_CHUNK];
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