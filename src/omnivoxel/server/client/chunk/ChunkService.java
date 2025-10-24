package omnivoxel.server.client.chunk;

import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.boundingBox.WorldBoundingBox;

import java.util.Set;

public class ChunkService {
    private final ChunkGenerator chunkGenerator;

    public ChunkService(ServerWorldDataService worldDataService, ServerBlockService blockService, ServerWorld world, Set<WorldBoundingBox> worldBoundingBoxes) {
        this.chunkGenerator = new ChunkGenerator(worldDataService, blockService, world, worldBoundingBoxes);
    }

    public void get(ChunkTask chunkTask) {
        // Check if the chunk is generated
        // If it isn't, generate it and store it
        // Find the neighboring chunks for the blocks
    }
}