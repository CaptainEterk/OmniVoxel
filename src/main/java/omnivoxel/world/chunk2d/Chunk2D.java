package omnivoxel.world.chunk2d;

public interface Chunk2D<B> {
    B getBlock(int x, int z);

    Chunk2D<B> setBlock(int x, int z, B block);
}