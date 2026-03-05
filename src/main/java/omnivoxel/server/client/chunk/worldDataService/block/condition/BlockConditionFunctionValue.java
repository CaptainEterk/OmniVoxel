package omnivoxel.server.client.chunk.worldDataService.block.condition;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.util.game.nodes.GameNode;

public abstract class BlockConditionFunctionValue {
    public BlockConditionFunctionValue(GameNode args, long i) {
        if (!this.getClass().isAnnotationPresent(Function.class)) {
            throw new IllegalStateException(
                    this.getClass().getName() + " must be annotated with @Function"
            );
        }
    }

    public abstract double evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z);
}