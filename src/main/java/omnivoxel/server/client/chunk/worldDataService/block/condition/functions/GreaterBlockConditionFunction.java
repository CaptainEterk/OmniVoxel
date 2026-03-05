package omnivoxel.server.client.chunk.worldDataService.block.condition.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunction;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunctionValue;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "greater")
public class GreaterBlockConditionFunction extends BlockConditionFunction {
    private final BlockConditionFunctionValue arg1;
    private final BlockConditionFunctionValue arg2;

    public GreaterBlockConditionFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
        arg1 = BlockConditionFunction.getValue(objectGameNode.object().get("arg1"), seed);
        arg2 = BlockConditionFunction.getValue(objectGameNode.object().get("arg2"), seed);
    }

    @Override
    public boolean evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return arg1.evaluate(density, info, floor, ceiling, depth, x, y, z) > arg2.evaluate(density, info, floor, ceiling, depth, x, y, z);
    }
}