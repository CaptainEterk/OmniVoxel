package omnivoxel.client.network.chunk.worldDataService;

import omnivoxel.client.game.graphics.block.BlockMesh;

import java.util.HashMap;
import java.util.Map;


// TODO: Merge with BlockService
public class ClientWorldDataService {
    private final Map<String, BlockMesh> blocks;

    public ClientWorldDataService() {
        blocks = new HashMap<>();
    }

    public BlockMesh getBlock(String blockModID) {
        return blocks.get(blockModID);
    }

    public void addBlock(BlockMesh blockMesh) {
        blocks.put(blockMesh.getModID() + "/" + blockMesh.getState(), blockMesh);
    }
}