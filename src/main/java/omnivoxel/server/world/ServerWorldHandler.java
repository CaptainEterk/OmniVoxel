package omnivoxel.server.world;

import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.block.ServerBlockAndPosition;
import omnivoxel.server.client.chunk.ChunkIO;
import omnivoxel.server.client.chunk.ChunkTask;
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

    public ServerWorldHandler(ServerWorld world, Map<String, ServerClient> clients, WorkerThreadPool<ChunkTask> workerThreadPool) {
        this.world = world;
        this.clients = clients;
        this.workerThreadPool = workerThreadPool;
    }

    public void replaceBlock(int worldX, int worldY, int worldZ, ServerBlock block, ServerClient client) {
        try {
            if (canModify(worldX, worldY, worldZ, client)) {
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
                    world.put(position3D, chunk.setBlock(x, y, z, block));

                    Position2D position2D = position3D.getPosition2D();
                    Chunk2D<Integer> chunkHeights = world.getChunkHeights(position2D);
                    int currentHighestY = chunkHeights.getBlock(x, z);
                    // TODO: Don't hardcode "omnivoxel:air"
                    if (Objects.equals(block.id(), "omnivoxel:air")) {
                        if (worldY == currentHighestY) {
                            world.putChunkHeights(position2D, chunkHeights.setBlock(x, z, worldY));
                        }
                    } else {
                        if (worldY > currentHighestY) {
                            world.putChunkHeights(position2D, chunkHeights.setBlock(x, z, worldY));
                        }
                    }
                    clients.forEach((id, serverClient) -> serverClient.queueReplacedBlocks(new ServerBlockAndPosition(worldX, worldY, worldZ, block)));
                } else {
                    ChunkTask task = new ChunkTask(null, chunkX, chunkY, chunkZ);
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
