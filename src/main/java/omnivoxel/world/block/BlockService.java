package omnivoxel.world.block;

import java.util.HashMap;
import java.util.Map;

public final class BlockService {
    private final Map<String, Block> serverBlocksById;

    public BlockService() {
        serverBlocksById = new HashMap<>();
    }

    public Block getBlock(String id) {
        Block block = serverBlocksById.get(id);

        if (block == null) {
            block = new Block(id);
            serverBlocksById.put(id, block);
        }

        return block;
    }
}