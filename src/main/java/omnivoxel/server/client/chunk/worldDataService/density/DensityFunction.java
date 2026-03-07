package omnivoxel.server.client.chunk.worldDataService.density;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.util.game.nodes.GameNode;

public abstract class DensityFunction {
    @SuppressWarnings("unused")
    public DensityFunction(GameNode args, long seed) {
        if (!this.getClass().isAnnotationPresent(Function.class)) {
            throw new IllegalStateException(
                    this.getClass().getName() + " must be annotated with @Function"
            );
        }
    }

    public abstract double evaluate(double x, double y, double z);
}