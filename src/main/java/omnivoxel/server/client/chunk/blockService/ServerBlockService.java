package omnivoxel.server.client.chunk.blockService;

import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.util.log.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerBlockService {
    private final Map<String, ServerBlock> serverBlocksById;

    public ServerBlockService() {
        serverBlocksById = new ConcurrentHashMap<>();
    }

    public ServerBlock getBlock(String id) {
        ServerBlock serverBlock = serverBlocksById.get(id);
        if (serverBlock == null) {
            serverBlocksById.put(id, ServerBlock.AIR);
            Logger.warn("Unregistered block: " + id);
        }

        return serverBlock;
    }

    public Map<String, ServerBlock> getAllBlocks() {
        return serverBlocksById;
    }

    public void registerServerBlock(ServerBlock serverBlock) {
        Logger.debug("Registered block: " + serverBlock.id());
        serverBlocksById.put(serverBlock.id(), serverBlock);
    }
}
