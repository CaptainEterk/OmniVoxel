package omnivoxel.world.chunk;

public interface Chunk<B> {
    B getBlock(int x, int y, int z);

    Chunk<B> setBlock(int x, int y, int z, B block);
}