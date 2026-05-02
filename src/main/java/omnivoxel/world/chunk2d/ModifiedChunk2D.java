package omnivoxel.world.chunk2d;

import omnivoxel.common.settings.ConstantCommonSettings;

public class ModifiedChunk2D<B> implements Chunk2D<B> {
    private final int x;
    private final int z;
    private final B block;
    private final Chunk2D<B> chunk;
    private final int modificationCount;

    private ModifiedChunk2D(int x, int z, B block, Chunk2D<B> chunk, int modificationCount) {
        this.x = x;
        this.z = z;
        this.block = block;
        this.chunk = chunk;
        this.modificationCount = modificationCount;
    }

    public ModifiedChunk2D(int x, int z, B block, Chunk2D<B> chunk) {
        this(x, z, block, chunk, 1);
    }

    @Override
    public B getBlock(int x, int z) {
        if (x == this.x && z == this.z) {
            return block;
        }
        return chunk.getBlock(x, z);
    }

    @Override
    public Chunk2D<B> setBlock(int x, int z, B block) {
        if (modificationCount > ConstantCommonSettings.MODIFICATION_GENERALIZATION_LIMIT) {
            return new ShortPaletteChunk2D<>(this).setBlock(x, z, block);
        }
        return new ModifiedChunk2D<>(x, z, block, this, modificationCount + 1);
    }
}