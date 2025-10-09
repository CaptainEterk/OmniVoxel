package omnivoxel.server.client.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.result.ChunkCacheItem;
import omnivoxel.server.client.chunk.result.ChunkResult;
import omnivoxel.server.client.chunk.result.GeneratedChunk;
import omnivoxel.server.client.chunk.worldDataService.ChunkInfo;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.world.ChunkCacheHandler;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.boundingBox.WorldBoundingBox;
import omnivoxel.util.cache.LRUCache;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;

import java.io.IOException;
import java.util.Set;

public final class ChunkGenerator {
    private final ServerWorldDataService worldDataService;
    private final ServerWorld world;
    private final Set<WorldBoundingBox> worldBoundingBoxes;
    private final LRUCache<Position2D, ChunkInfo> chunkInfoLRUCache = new LRUCache<>(100);

    public ChunkGenerator(ServerWorldDataService worldDataService, ServerBlockService blockService, ServerWorld world, Set<WorldBoundingBox> worldBoundingBoxes) {
        this.worldDataService = worldDataService;
        this.world = world;
        this.worldBoundingBoxes = worldBoundingBoxes;
    }

    private void sendChunkBytes(ChannelHandlerContext ctx, int x, int y, int z, byte[] chunk) {
        ByteBuf buffer = Unpooled.buffer();
        int length = 16 + chunk.length;
        buffer.writeInt(length);
        buffer.writeInt(PackageID.CHUNK.ordinal());
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeBytes(chunk);
        ctx.channel().writeAndFlush(buffer);
    }

    public void generateChunk(ChunkTask task) {
        Position3D position3D = new Position3D(task.x(), task.y(), task.z());

        try {
            byte[] chunkBytes = world.getBytes(position3D);
            if (chunkBytes != null) {
                sendChunkBytes(task.serverClient().getCTX(), task.x(), task.y(), task.z(), chunkBytes);
            } else {
                ChunkResult chunkResult;
                GeneratedChunk chunk = new EmptyGeneratedChunk();
                if (worldDataService.shouldGenerateChunk(position3D)) {
                    Position2D position2D = position3D.getPosition2D();
                    ChunkInfo chunkInfo = chunkInfoLRUCache.get(position2D);
                    chunkInfo = chunkInfo == null ? worldDataService.getChunkInfo(position3D) : chunkInfo;
                    for (int x = -1; x < ConstantGameSettings.CHUNK_WIDTH + 1; x++) {
                        int worldX = position3D.x() * ConstantGameSettings.CHUNK_WIDTH + x;
                        for (int z = -1; z < ConstantGameSettings.CHUNK_LENGTH + 1; z++) {
                            int worldZ = position3D.z() * ConstantGameSettings.CHUNK_LENGTH + z;

                            for (int y = -1; y < ConstantGameSettings.CHUNK_HEIGHT + 1; y++) {
                                int worldY = position3D.y() * ConstantGameSettings.CHUNK_HEIGHT + y;

                                boolean border =
                                        x == -1 || x == ConstantGameSettings.CHUNK_WIDTH ||
                                                y == -1 || y == ConstantGameSettings.CHUNK_HEIGHT ||
                                                z == -1 || z == ConstantGameSettings.CHUNK_LENGTH;

                                chunk = chunk.setBlock(x, y, z, worldDataService.getBlockAt(x, y, z, worldX, worldY, worldZ, border, chunkInfo));
                            }
                        }
                    }

                    chunkInfoLRUCache.put(position3D.getPosition2D(), chunkInfo);
                }

                chunkResult = GeneratedChunk.getResult(chunk, task.serverClient());
                world.add(position3D, chunkResult.chunk());

                ChunkCacheHandler.queueCache(new ChunkCacheItem(position3D, chunkResult.bytes()));

                sendChunkBytes(task.serverClient().getCTX(), task.x(), task.y(), task.z(), chunkResult.bytes());
            }
        } catch (IOException e) {
            task.byteBuf().release();
            throw new RuntimeException(e);
        }
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