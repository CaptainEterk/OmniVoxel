package omnivoxel.server.client.chunk.result.generated;

import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.server.client.block.ServerBlock;

public class ModifiedGeneratedChunk extends GeneratedChunk {
    private final int x;
    private final int y;
    private final int z;
    private final ServerBlock block;
    private final GeneratedChunk chunk;
    private final int modificationCount;

    private ModifiedGeneratedChunk(int x, int y, int z, ServerBlock block, GeneratedChunk chunk, int modificationCount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.chunk = chunk;
        this.modificationCount = modificationCount;
    }

    public ModifiedGeneratedChunk(int x, int y, int z, ServerBlock block, GeneratedChunk chunk) {
        this(x, y, z, block, chunk, 1);
    }

    @Override
    public ServerBlock getBlock(int x, int y, int z) {
        if (x == this.x && y == this.y && z == this.z) {
            return block;
        }
        return chunk.getBlock(x, y, z);
    }

    @Override
    public GeneratedChunk setBlock(int x, int y, int z, @NotNull ServerBlock block) {
        if (modificationCount > ConstantCommonSettings.MODIFICATION_GENERALIZATION_LIMIT) {
            return new GeneralGeneratedChunk(this);
        }
        return new ModifiedGeneratedChunk(x, y, z, block, this, modificationCount + 1);
    }
}