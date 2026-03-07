package omnivoxel.world.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.util.IndexCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortPaletteChunk<B> implements Chunk<B> {
    private final short[] blocks;
    private final List<B> palette;
    private final Map<B, Short> paletteIndex;

    public ShortPaletteChunk(Chunk<B> chunk) {
        this.palette = new ArrayList<>();
        this.paletteIndex = new HashMap<>();
        this.blocks = extractBlocks(chunk);
    }

    private short[] extractBlocks(Chunk<B> chunk) {
        short[] blocks = new short[ConstantGameSettings.BLOCKS_IN_CHUNK];

        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {

                    B block = chunk.getBlock(x, y, z);

                    Short index = paletteIndex.get(block);
                    if (index == null) {
                        index = (short) palette.size();
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

        Short index = paletteIndex.get(block);
        if (index == null) {
            index = (short) palette.size();
            palette.add(block);
            paletteIndex.put(block, index);
        }

        blocks[blockIndex] = index;

        if (palette.size() > Short.MAX_VALUE - 2) {
            return new IntPaletteChunk<>(this);
        }

        return this;
    }
}