package omnivoxel.server.client.chunk.result.generated;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.util.IndexCalculator;
import org.jetbrains.annotations.NotNull;

public class GeneralGeneratedChunk extends GeneratedChunk {
    private final ServerBlock[] blocks;

    public GeneralGeneratedChunk() {
        blocks = new ServerBlock[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];
    }

    public GeneralGeneratedChunk(GeneratedChunk chunk) {
        this.blocks = extractBlocks(chunk);
    }

    private ServerBlock[] extractBlocks(GeneratedChunk chunk) {
        ServerBlock[] blocks = new ServerBlock[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];
        for (int x = -1; x <= ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = -1; z <= ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = -1; y <= ConstantGameSettings.CHUNK_HEIGHT; y++) {
                    blocks[IndexCalculator.calculateBlockIndexPadded(x, y, z)] = chunk.getBlock(x, y, z);
                }
            }
        }
        return blocks;
    }

    public ServerBlock getBlock(int x, int y, int z) {
        return blocks[IndexCalculator.calculateBlockIndexPadded(x, y, z)];
    }

    public GeneratedChunk setBlock(int x, int y, int z, @NotNull ServerBlock block) {
        blocks[IndexCalculator.calculateBlockIndexPadded(x, y, z)] = block;
        return this;
    }
}