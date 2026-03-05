package omnivoxel.client.network.chunk.worldDataService;

import omnivoxel.client.game.graphics.opengl.mesh.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientWorldDataService {
    private final Map<String, Block> blocks;

    public ClientWorldDataService() {
        blocks = new ConcurrentHashMap<>();
    }

    public Block getBlock(String blockModID) {
        return blocks.get(blockModID);
    }

    public void addBlock(Block block) {
        blocks.put(block.getModID() + "/" + block.getState(), block);
    }
}