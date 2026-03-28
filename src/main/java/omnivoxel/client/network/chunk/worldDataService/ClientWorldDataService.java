package omnivoxel.client.network.chunk.worldDataService;

import omnivoxel.client.game.graphics.api.opengl.mesh.block.BlockMesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// TODO: Merge with BlockService
public class ClientWorldDataService {
    private final Map<String, BlockMesh> blocks;

    public ClientWorldDataService() {
        blocks = new ConcurrentHashMap<>();
    }

    public BlockMesh getBlock(String blockModID) {
        return blocks.get(blockModID);
    }

    public void addBlock(BlockMesh blockMesh) {
        blocks.put(blockMesh.getModID() + "/" + blockMesh.getState(), blockMesh);
    }
}