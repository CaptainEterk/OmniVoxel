package omnivoxel.server.client.chunk.worldDataService.block.condition.values;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunctionValue;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "value")
public class ValueBlockConditionFunctionValue extends BlockConditionFunctionValue {
    private final double value;

    public ValueBlockConditionFunctionValue(GameNode args, long i) {
        super(args, i);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
        this.value = Game.checkGameNodeType(objectGameNode.object().get("arg"), DoubleGameNode.class).value();
    }

    @Override
    public double evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return value;
    }
}