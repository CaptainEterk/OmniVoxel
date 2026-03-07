package omnivoxel.server.client.chunk.worldDataService.block.condition.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunction;
import omnivoxel.util.game.nodes.GameNode;

@Function(id = "is_floor")
public class FloorBlockConditionFunction extends BlockConditionFunction {
    public FloorBlockConditionFunction(GameNode args, long i) {
        super(args, i);
    }

    @Override
    public boolean evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return floor;
    }
}