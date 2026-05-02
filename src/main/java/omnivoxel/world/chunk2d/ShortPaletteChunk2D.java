package omnivoxel.world.chunk2d;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortPaletteChunk2D<B> implements Chunk2D<B> {
    private final short[] blocks;
    private final List<B> palette;
    private final Map<B, Short> paletteIndex;

    public ShortPaletteChunk2D(Chunk2D<B> chunk) {
        this.palette = new ArrayList<>();
        this.paletteIndex = new HashMap<>();
        this.blocks = extractBlocks(chunk);
    }

    private short[] extractBlocks(Chunk2D<B> chunk) {
        short[] blocks = new short[ConstantCommonSettings.BLOCKS_IN_CHUNK_2D];

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                B block = chunk.getBlock(x, z);

                Short index = paletteIndex.get(block);
                if (index == null) {
                    index = (short) palette.size();
                    palette.add(block);
                    paletteIndex.put(block, index);
                }

                blocks[IndexCalculator.calculateBlockIndex2D(x, z)] = index;
            }
        }

        return blocks;
    }

    @Override
    public B getBlock(int x, int z) {
        return palette.get(blocks[IndexCalculator.calculateBlockIndex2D(x, z)]);
    }

    @Override
    public Chunk2D<B> setBlock(int x, int z, B block) {
        int blockIndex = IndexCalculator.calculateBlockIndex2D(x, z);

        Short index = paletteIndex.get(block);
        if (index == null) {
            index = (short) palette.size();
            palette.add(block);
            paletteIndex.put(block, index);
        }

        blocks[blockIndex] = index;

        if (palette.size() > Short.MAX_VALUE - 2) {
            return new IntPaletteChunk2D<>(this);
        }

        return this;
    }
}