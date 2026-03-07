package omnivoxel.server.client.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.result.generated.EmptyGeneratedChunk;
import omnivoxel.server.client.chunk.result.generated.GeneratedChunk;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

/**
 * Utility for constructing padded (34x34x34) chunks by merging
 * the target chunk with its 1-block neighbors on all sides.
 * <p>
 * This is used for client-side mesh generation so face culling
 * can access neighboring block data without extra chunk lookups.
 */
public final class ChunkPacker {
    private ChunkPacker() {
    }

    /**
     * Builds a padded chunk using a preloaded 3x3x3 array of chunks.
     * This avoids redundant world lookups.
     */
    public static GeneratedChunk buildPaddedChunk(Position3D centerPos,
                                                  Chunk<ServerBlock>[][][] neighbors,
                                                  ServerWorld world) {

        GeneratedChunk padded = new EmptyGeneratedChunk();

        for (int x = -1; x <= ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int y = -1; y <= ConstantGameSettings.CHUNK_HEIGHT; y++) {
                for (int z = -1; z <= ConstantGameSettings.CHUNK_LENGTH; z++) {

                    int cx = (x < 0) ? -1 : (x == ConstantGameSettings.CHUNK_WIDTH ? 1 : 0);
                    int cy = (y < 0) ? -1 : (y == ConstantGameSettings.CHUNK_HEIGHT ? 1 : 0);
                    int cz = (z < 0) ? -1 : (z == ConstantGameSettings.CHUNK_LENGTH ? 1 : 0);

                    Chunk<ServerBlock> src = neighbors[cx + 1][cy + 1][cz + 1];
                    if (src == null) {
                        padded.setBlock(x, y, z, ServerBlock.AIR);
                        continue;
                    }

                    int bx = (x + ConstantGameSettings.CHUNK_WIDTH) % ConstantGameSettings.CHUNK_WIDTH;
                    int by = (y + ConstantGameSettings.CHUNK_HEIGHT) % ConstantGameSettings.CHUNK_HEIGHT;
                    int bz = (z + ConstantGameSettings.CHUNK_LENGTH) % ConstantGameSettings.CHUNK_LENGTH;

                    ServerBlock block = src.getBlock(bx, by, bz);
                    padded.setBlock(x, y, z, block);
                }
            }
        }

        return padded;
    }
}