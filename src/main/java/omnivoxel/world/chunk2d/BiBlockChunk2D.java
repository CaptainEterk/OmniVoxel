package omnivoxel.world.chunk2d;

import omnivoxel.client.game.settings.ConstantGameSettings;

import java.util.Objects;

public class BiBlockChunk2D<B> implements Chunk2D<B> {
    private final int[] blocks;
    private final B block1;
    private B block2 = null;

    public BiBlockChunk2D(B block) {
        this.block1 = block;
        blocks = new int[ConstantGameSettings.CHUNK_LENGTH];
    }

    @Override
    public B getBlock(int x, int z) {
        return (blocks[z] & (1 << x)) != 0 ? this.block2 : block1;
    }

    @Override
    public Chunk2D<B> setBlock(int x, int z, B block) {
        if (Objects.equals(this.block1, block)) {
            blocks[z] &= ~(1 << x);
        } else if (this.block2 == null) {
            this.block2 = block;
            blocks[z] |= (1 << x);
        } else if (Objects.equals(this.block2, block)) {
            blocks[z] |= (1 << x);
        } else {
            return new ModifiedChunk2D<>(x, z, block, this);
        }
        return this;
    }
}
