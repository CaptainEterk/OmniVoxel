package omnivoxel.server.client.chunk.worldDataService.block.condition.values;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunctionValue;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

@Function(id = "var")
public class VariableBlockConditionFunctionValue extends BlockConditionFunctionValue {
    private final ValueType valueType;

    public VariableBlockConditionFunctionValue(GameNode args, long i) {
        super(args, i);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
        this.valueType = ValueType.valueOf(Game.checkGameNodeType(objectGameNode.object().get("arg"), StringGameNode.class).value().toUpperCase());
    }

    @Override
    public double evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return switch (valueType) {
            case DENSITY -> density;
            case X -> x;
            case Y -> y;
            case Z -> z;
            case DEPTH -> depth;
        };
    }

    private enum ValueType {
        DENSITY,
        X,
        Y,
        Z,
        DEPTH
    }
}
