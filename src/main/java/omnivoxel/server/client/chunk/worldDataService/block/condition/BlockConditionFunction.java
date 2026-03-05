package omnivoxel.server.client.chunk.worldDataService.block.condition;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.values.ValueBlockConditionFunctionValue;
import omnivoxel.server.client.chunk.worldDataService.block.condition.values.VariableBlockConditionFunctionValue;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class BlockConditionFunction {
    private static final Map<String, Class<? extends BlockConditionFunctionValue>> blockConditionFunctionValueCache = new HashMap<>();

    static {
        addBlockConditionValue(VariableBlockConditionFunctionValue.class);
        addBlockConditionValue(ValueBlockConditionFunctionValue.class);
    }

    public BlockConditionFunction(GameNode args, long i) {
        if (!this.getClass().isAnnotationPresent(Function.class)) {
            throw new IllegalStateException(
                    this.getClass().getName() + " must be annotated with @Function"
            );
        }
    }

    private static void addBlockConditionValue(Class<? extends BlockConditionFunctionValue> blockConditionFunctionValueClass) {
        Function[] annotations = blockConditionFunctionValueClass.getAnnotationsByType(Function.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException("Block condition function values must have the @Function annotation");
        }
        blockConditionFunctionValueCache.put(annotations[0].id(), blockConditionFunctionValueClass);
    }

    protected static BlockConditionFunctionValue getValue(GameNode args, long seed) {
        try {
            String type = Game.checkGameNodeType(Game.checkGameNodeType(args, ObjectGameNode.class).object().get("type"), StringGameNode.class).value();
            Class<? extends BlockConditionFunctionValue> dfClass = blockConditionFunctionValueCache.get(type);
            if (dfClass == null) {
                throw new IllegalArgumentException(String.format("%s is not a valid type for a density function", type));
            }
            return dfClass.getConstructor(GameNode.class, long.class).newInstance(args, seed);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract boolean evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z);
}