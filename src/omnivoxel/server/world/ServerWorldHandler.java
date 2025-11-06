package omnivoxel.server.world;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.ServerLogger;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.block.ServerBlockAndPosition;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

import java.util.Map;

public class ServerWorldHandler {
    private final ServerWorld world;
    private final Map<String, ServerClient> clients;

    public ServerWorldHandler(ServerWorld world, Map<String, ServerClient> clients) {
        this.world = world;
        this.clients = clients;
    }

    public void replaceBlock(int worldX, int worldY, int worldZ, ServerBlock block, ServerClient client) {
        if (canModify(worldX, worldY, worldZ, client)) {
            int chunkX = worldX / ConstantGameSettings.CHUNK_WIDTH;
            int chunkY = worldY / ConstantGameSettings.CHUNK_HEIGHT;
            int chunkZ = worldZ / ConstantGameSettings.CHUNK_LENGTH;
            int x = Math.floorMod(worldX, ConstantGameSettings.CHUNK_WIDTH);
            int y = Math.floorMod(worldY, ConstantGameSettings.CHUNK_HEIGHT);
            int z = Math.floorMod(worldZ, ConstantGameSettings.CHUNK_LENGTH);
            Position3D position3D = new Position3D(chunkX, chunkY, chunkZ);
            Chunk<ServerBlock> chunk = world.get(position3D);
            if (chunk != null) {
                world.put(position3D, chunk.setBlock(x, y, z, block));

                clients.forEach((id, serverClient) -> serverClient.queueReplacedBlocks(new ServerBlockAndPosition(worldX, worldY, worldZ, block)));
            } else {
                ServerLogger.logger.debug("Unable to set block (%d, %d, %d)".formatted(worldX, worldY, worldZ));
            }
        }
    }

    private boolean canModify(int worldX, int worldY, int worldZ, ServerClient client) {
        // TODO: Sometimes the region might check the name of the player
        return true;
    }
}