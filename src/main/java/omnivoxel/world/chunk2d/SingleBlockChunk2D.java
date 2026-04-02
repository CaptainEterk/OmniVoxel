package omnivoxel.world.chunk2d;

public class SingleBlockChunk2D<B> implements Chunk2D<B> {
    private final B block;

    public SingleBlockChunk2D(B block) {
        this.block = block;
    }

    @Override
    public B getBlock(int x, int z) {
        return block;
    }

    @Override
    public Chunk2D<B> setBlock(int x, int z, B block) {
        if (this.block == block) {
            return this;
        }
        return new BiBlockChunk2D<>(this.block).setBlock(x, z, block);
    }
}
