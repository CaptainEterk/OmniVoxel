package omnivoxel.client.game.graphics.opengl.mesh.generators;

import omnivoxel.client.game.graphics.opengl.mesh.block.Block;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.world.chunk.Chunk;

import java.util.HashSet;
import java.util.Set;

public record ChunkBlockData(Chunk<omnivoxel.world.block.Block> chunk, Block[] blocks) {

    /**
     * Generates a ChunkBlockData instance from a Chunk<Block> by creating a palette
     * of unique blocks and returning them in array form.
     */
    public static ChunkBlockData fromChunk(Chunk<omnivoxel.world.block.Block> chunk, ClientWorldDataService worldDataService) {
        Set<Block> palette = new HashSet<>();

        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                    palette.add(worldDataService.getBlock(chunk.getBlock(x, y, z).id()));
                }
            }
        }

        Block[] blocksArray = palette.toArray(
                new Block[0]
        );

        return new ChunkBlockData(chunk, blocksArray);
    }
}