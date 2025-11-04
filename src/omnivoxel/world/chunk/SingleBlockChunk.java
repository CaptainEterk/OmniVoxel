package omnivoxel.world.chunk;

public class SingleBlockChunk<B> implements Chunk<B> {
    private final B block;

    public SingleBlockChunk(B block) {
        this.block = block;
    }

    @Override
    public B getBlock(int x, int y, int z) {
        return block;
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {
        if (this.block == block) {
            return this;
        }
        return new BiBlockChunk<>(block).setBlock(x, y, z, block);
    }
}