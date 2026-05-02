package omnivoxel.client.network.request;

import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;

public record BlockReplaceRequest(Position3D position3D, Block newBlock) implements Request {
    @Override
    public RequestType getType() {
        return RequestType.BLOCK_REPLACE;
    }
}