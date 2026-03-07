package omnivoxel.server.client.chunk.worldDataService.block.condition.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunction;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunctionValue;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "less")
public class LessBlockConditionFunction extends BlockConditionFunction {
    private final BlockConditionFunctionValue arg1;
    private final BlockConditionFunctionValue arg2;

    public LessBlockConditionFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            arg1 = BlockConditionFunction.getValue(objectGameNode.object().get("arg1"), seed);
            arg2 = BlockConditionFunction.getValue(objectGameNode.object().get("arg2"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public boolean evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return arg1.evaluate(density, info, floor, ceiling, depth, x, y, z) < arg2.evaluate(density, info, floor, ceiling, depth, x, y, z);
    }
}