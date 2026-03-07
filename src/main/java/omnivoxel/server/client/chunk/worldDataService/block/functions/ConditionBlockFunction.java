package omnivoxel.server.client.chunk.worldDataService.block.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.block.BlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunction;
import omnivoxel.server.client.chunk.worldDataService.block.condition.functions.*;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Function(id = "condition")
public class ConditionBlockFunction extends BlockFunction {
    private static final Map<String, Class<? extends BlockConditionFunction>> blockConditionFunctionCache = new HashMap<>();

    static {
        addBlockConditionFunction(GreaterBlockConditionFunction.class);
        addBlockConditionFunction(LessBlockConditionFunction.class);
        addBlockConditionFunction(YGradientBlockConditionFunction.class);
        addBlockConditionFunction(FloorBlockConditionFunction.class);
        addBlockConditionFunction(CeilingBlockConditionFunction.class);
        addBlockConditionFunction(EqualBlockConditionFunction.class);
    }

    private final BlockConditionFunction condition;
    private final BlockFunction ifTrue;

    public ConditionBlockFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
        condition = getConditionFunction(Game.checkGameNodeType(objectGameNode.object().get("condition"), ObjectGameNode.class), seed);
        ifTrue = ServerWorldDataService.getBlockFunction(objectGameNode.object().get("if_true"), seed);
    }

    private static void addBlockConditionFunction(Class<? extends BlockConditionFunction> blockConditionFunctionValueClass) {
        Function[] annotations = blockConditionFunctionValueClass.getAnnotationsByType(Function.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException("Block condition functions must have the @Function annotation");
        }
        blockConditionFunctionCache.put(annotations[0].id(), blockConditionFunctionValueClass);
    }

    public static BlockConditionFunction getConditionFunction(GameNode args, long seed) {
        try {
            ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
            String type = Game.checkGameNodeType(objectGameNode.object().get("type"), StringGameNode.class).value();

            Class<? extends BlockConditionFunction> dfClass = blockConditionFunctionCache.get(type);
            if (dfClass == null) {
                throw new IllegalArgumentException(String.format("%s is not a valid type for a block function", type));
            }
            return dfClass.getConstructor(GameNode.class, long.class).newInstance(args, seed);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return condition.evaluate(density, info, floor, ceiling, depth, x, y, z) ? ifTrue.evaluate(density, info, floor, ceiling, depth, x, y, z) : null;
    }
}