package omnivoxel.server.world;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.block.ServerBlockAndPosition;
import omnivoxel.server.client.chunk.ChunkTask;
import omnivoxel.server.client.chunk.worldDataService.WorldGenerator;
import omnivoxel.server.io.chunk.ChunkIO;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ServerWorldHandler {
    private final ServerWorld world;
    private final Map<String, ServerClient> clients;
    private final WorkerThreadPool<ChunkTask> workerThreadPool;
    private final WorldGenerator worldGenerator;

    public ServerWorldHandler(ServerWorld world, Map<String, ServerClient> clients, WorkerThreadPool<ChunkTask> workerThreadPool, WorldGenerator worldGenerator) {
        this.world = world;
        this.clients = clients;
        this.workerThreadPool = workerThreadPool;
        this.worldGenerator = worldGenerator;
    }

    public void init() {
        workerThreadPool.submit(new ChunkTask(null, 0, 0, 0, 0));
        workerThreadPool.submit(new ChunkTask(null, -1, 0, 0, 0));
        workerThreadPool.submit(new ChunkTask(null, 0, 0, -1, 0));
        workerThreadPool.submit(new ChunkTask(null, -1, 0, -1, 0));

//        int count = 0;
////        for (int y = -10; y < 100; y++) {
//            for (int x = -100; x < 100; x++) {
//                for (int z = -100; z < 100; z++) {
//                    workerThreadPool.submit(new ChunkTask(null, x, 0, z, 0));
//                    count++;
//                }
//            }
////        }
//        System.out.println(count);
    }

    public void replaceBlock(int worldX, int worldY, int worldZ, ServerBlock block, byte rotation, ServerClient client) {
        try {
            if (canModify(worldX, worldY, worldZ, client)) {
                rotation = block.rotatable() ? (byte) (rotation & 3) : 0;
                int chunkX = IndexCalculator.chunkX(worldX);
                int chunkY = IndexCalculator.chunkY(worldY);
                int chunkZ = IndexCalculator.chunkZ(worldZ);
                int x = IndexCalculator.localX(worldX);
                int y = IndexCalculator.localY(worldY);
                int z = IndexCalculator.localZ(worldZ);
                Position3D position3D = new Position3D(chunkX, chunkY, chunkZ);
                Chunk<ServerBlock> chunk = world.get(position3D);
                if (chunk == null) {
                    chunk = ChunkIO.decode(ChunkIO.get(position3D));
                }
                if (chunk != null) {
                    if (!Objects.equals(chunk.getBlock(x, y, z).id(), block.id()) || chunk.getBlockRotation(x, y, z) != rotation) {
                        chunk = chunk.setBlock(x, y, z, block, rotation);
                        world.put(position3D, chunk);
                        ChunkIO.writeChunk(position3D, chunk, true);

                        Logger.debug("Replacing block in chunk: " + chunkX + " " + chunkY + " " + chunkZ + " at " + x + " " + y + " " + z + " with " + block.id());

                        Position2D position2D = position3D.getPosition2D();
                        Chunk2D<Integer> chunkHeights = world.getStoredChunkHeights(position2D);
                        if (chunkHeights == null) {
                            Logger.warn("Chunk heights are null at " + position2D + ". Rebuilding heightmap...");
                            chunkHeights = worldGenerator.rebuildChunkHeights(world, position2D);
                        }
                        int currentHighestY = chunkHeights.getBlock(x, z);
                        boolean updateChunkHeights = false;
                        if (currentHighestY == worldY) {
                            int cachedChunkY = chunkY;
                            Chunk<ServerBlock> cachedChunk = chunk;
                            for (int hy = worldY - 1; hy > worldGenerator.getBlockMinY(); hy--) {
                                int highestChunkY = IndexCalculator.chunkY(hy);
                                if (cachedChunkY != highestChunkY) {
                                    cachedChunkY = highestChunkY;
                                    Position3D chunkPosition = new Position3D(chunkX, highestChunkY, chunkZ);
                                    cachedChunk = world.get(chunkPosition);
                                    if (cachedChunk == null) {
                                        cachedChunk = ChunkIO.decode(ChunkIO.get(chunkPosition));
                                    }
                                    if (cachedChunk == null) {
                                        hy -= ConstantCommonSettings.CHUNK_HEIGHT - 1;
                                        continue;
                                    }
                                }
                                if (cachedChunk.getBlock(x, IndexCalculator.localY(hy), z).partOfGround()) {
                                    chunkHeights = chunkHeights.setBlock(x, z, hy);
                                    updateChunkHeights = true;
                                    break;
                                }
                            }
                            if (!updateChunkHeights) {
                                chunkHeights = chunkHeights.setBlock(x, z, worldGenerator.getBlockMinY());
                                updateChunkHeights = true;
                            }
                        } else if (block.partOfGround() && worldY > currentHighestY) {
                            chunkHeights = chunkHeights.setBlock(x, z, worldY);
                            updateChunkHeights = true;
                        }

                        if (updateChunkHeights) {
                            world.putChunkHeights(position2D, chunkHeights);
                            ChunkIO.writeChunk2D(position2D, chunkHeights, true);
                        }
                        byte finalRotation = rotation;
                        clients.forEach((id, serverClient) -> serverClient.queueReplacedBlocks(new ServerBlockAndPosition(worldX, worldY, worldZ, block, finalRotation)));
                    }
                } else {
                    ChunkTask task = new ChunkTask(null, chunkX, chunkY, chunkZ, 0);
                    if (!workerThreadPool.hasTask(task)) {
                        // TODO: Make it so that generating the chunk if you can't set the block is a setting
                        Logger.debug("Unable to set block (%d, %d, %d) because chunk (%d, %d, %d) is null... generating...".formatted(worldX, worldY, worldZ, chunkX, chunkY, chunkZ));
                        workerThreadPool.submit(task);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean canModify(int worldX, int worldY, int worldZ, ServerClient client) {
        // TODO: Sometimes the region might check the name of the player
        return true;
    }
}
