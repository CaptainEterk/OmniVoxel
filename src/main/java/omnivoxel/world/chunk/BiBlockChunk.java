package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;

import java.util.Objects;

public class BiBlockChunk<B> implements Chunk<B> {
    private final int[] blocks;
    private final B block1;
    private B block2 = null;

    public BiBlockChunk(B block) {
        this.block1 = block;
        blocks = new int[ConstantGameSettings.CHUNK_WIDTH * ConstantGameSettings.CHUNK_LENGTH];
    }

    @Override
    public B getBlock(int x, int y, int z) {
        return (blocks[z * ConstantGameSettings.CHUNK_WIDTH + x] & (1 << y)) != 0 ? this.block2 : block1;
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {
        if (Objects.equals(this.block1, block)) {
            blocks[z * ConstantGameSettings.CHUNK_WIDTH + x] &= ~(1 << y);
        } else if (this.block2 == null) {
            this.block2 = block;
            blocks[z * ConstantGameSettings.CHUNK_WIDTH + x] |= (1 << y);
        } else if (Objects.equals(this.block2, block)) {
            blocks[z * ConstantGameSettings.CHUNK_WIDTH + x] |= (1 << y);
        } else {
            return new ModifiedChunk<>(x, y, z, block, this);
        }
        return this;
    }
}