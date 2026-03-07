package omnivoxel.server.client.chunk;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.ChunkInfo;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.boundingBox.WorldBoundingBox;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.SingleBlockChunk;

import java.util.Set;

public final class ChunkGenerator {
    private final ServerWorldDataService worldDataService;
    private final ServerWorld world;
    private final Set<WorldBoundingBox> worldBoundingBoxes;

    public ChunkGenerator(ServerWorldDataService worldDataService, ServerBlockService blockService, ServerWorld world, Set<WorldBoundingBox> worldBoundingBoxes) {
        this.worldDataService = worldDataService;
        this.world = world;
        this.worldBoundingBoxes = worldBoundingBoxes;
    }

    public Chunk<ServerBlock> generateChunk(int cx, int cy, int cz) {
        Position3D position3D = new Position3D(cx, cy, cz);

        Chunk<ServerBlock> chunk = new SingleBlockChunk<>(ServerBlock.VOID);
        if (worldDataService.shouldGenerateChunk(position3D)) {
            ChunkInfo chunkInfo = worldDataService.getChunkInfo(position3D);
            for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
                int worldX = position3D.x() * ConstantGameSettings.CHUNK_WIDTH + x;
                for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                    int worldZ = position3D.z() * ConstantGameSettings.CHUNK_LENGTH + z;
                    for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {
                        int worldY = position3D.y() * ConstantGameSettings.CHUNK_HEIGHT + y;
                        chunk = chunk.setBlock(x, y, z, worldDataService.getBlockAt(x, y, z, worldX, worldY, worldZ, chunkInfo));
                    }
                }
            }
        }

        return chunk;
    }

//    private void generateSurroundingChunks(Position3D position3D, int size) {
//        for (int x = -size; x <= size; x++) {
//            for (int y = -size; y <= size; y++) {
//                for (int z = -size; z <= size; z++) {
//                    if (x == 0 && y == 0 && z == 0) {
//                        continue;
//                    }
//
//                    generateChunkStructures(position3D.add(x, y, z));
//                }
//            }
//        }
//    }

//    private void generateChunkStructures(Position3D position3D) {
//        if (structureGeneratedChunks.add(position3D)) {
//            ChunkInfo chunkInfo = worldDataService.getChunkInfo(position3D);
//
//            for (int x = -1; x < ConstantGameSettings.CHUNK_WIDTH + 1; x++) {
//                int worldX = position3D.x() * ConstantGameSettings.CHUNK_WIDTH + x;
//                for (int z = -1; z < ConstantGameSettings.CHUNK_LENGTH + 1; z++) {
//                    int worldZ = position3D.z() * ConstantGameSettings.CHUNK_LENGTH + z;
//                    for (int y = -1; y < ConstantGameSettings.CHUNK_HEIGHT + 1; y++) {
//                        int worldY = position3D.y() * ConstantGameSettings.CHUNK_HEIGHT + y;
//
//                        StructureSeed structureSeed = structureService.getStructure(x, y, z, worldX, worldY, worldZ, chunkInfo);
//
//                        if (structureSeed != null) {
//                            Structure structure = structureSeed.structure();
//                            Map<Position3D, PriorityServerBlock> blocks = structure.getBlocks();
//                            StructureBoundingBox boundingBox = structure.getBoundingBox();
//
//                            // Convert bounding box to world-space bounds
//                            int minX = worldX;
//                            int maxX = worldX + boundingBox.getWidth();
//                            int minY = worldY;
//                            int maxY = worldY + boundingBox.getHeight();
//                            int minZ = worldZ;
//                            int maxZ = worldZ + boundingBox.getLength();
//
//                            WorldBoundingBox worldBoundingBox = new WorldBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
//
//                            boolean found = worldBoundingBoxes.stream().anyMatch(existing -> existing.intersects(worldBoundingBox));
//                            if (found) {
//                                continue;
//                            }
//
//                            worldBoundingBoxes.add(worldBoundingBox);
//
//                            Position3D origin = structure.getOrigin();
//                            if (structureSeed.offset() != null) {
//                                origin.add(structureSeed.offset());
//                            }
//                            blocks.forEach((blockPosition, priorityServerBlock) -> {
//                                Position3D pos = origin.add(blockPosition).add(worldX, worldY, worldZ);
//                                worldDataService.queueBlock(pos, priorityServerBlock);
//                            });
//                        }
//                    }
//                }
//            }
//        }
//    }
}