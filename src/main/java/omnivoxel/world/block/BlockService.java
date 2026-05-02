package omnivoxel.world.block;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class BlockService<B> {
    private final Map<String, B> blockByID;
    private final Function<String, B> blockFactory;

    public BlockService(Function<String, B> blockFactory) {
        this.blockFactory = blockFactory;
        blockByID = new HashMap<>();
    }

    public B getBlock(String id) {
        B block = blockByID.get(id);

        if (block == null) {
            block = blockFactory.apply(id);
            blockByID.put(id, block);
        }

        return block;
    }
}