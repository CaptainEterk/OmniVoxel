package omnivoxel.world.chunk;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntPaletteChunk<B> implements Chunk<B> {
    private final int[] blocks;
    private final List<B> palette;
    private final Map<B, Integer> paletteIndex;

    public IntPaletteChunk(Chunk<B> chunk) {
        this.palette = new ArrayList<>();
        this.paletteIndex = new HashMap<>();
        this.blocks = extractBlocks(chunk);
    }

    private int[] extractBlocks(Chunk<B> chunk) {
        int[] blocks = new int[ConstantCommonSettings.BLOCKS_IN_CHUNK];

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantCommonSettings.CHUNK_HEIGHT; y++) {

                    B block = chunk.getBlock(x, y, z);

                    Integer index = paletteIndex.get(block);
                    if (index == null) {
                        index = palette.size();
                        palette.add(block);
                        paletteIndex.put(block, index);
                    }

                    blocks[IndexCalculator.calculateBlockIndex(x, y, z)] = index;
                }
            }
        }

        return blocks;
    }

    @Override
    public B getBlock(int x, int y, int z) {
        return palette.get(blocks[IndexCalculator.calculateBlockIndex(x, y, z)]);
    }

    @Override
    public Chunk<B> setBlock(int x, int y, int z, B block) {
        int blockIndex = IndexCalculator.calculateBlockIndex(x, y, z);

        Integer index = paletteIndex.get(block);
        if (index == null) {
            index = palette.size();
            palette.add(block);
            paletteIndex.put(block, index);
        }

        blocks[blockIndex] = index;
        return this;
    }
}