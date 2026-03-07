package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;

@Function(id = "value")
public class ValueDensityFunction extends DensityFunction {
    private final double value;

    public ValueDensityFunction(GameNode args, long seed) {
        super(args, seed);

        this.value = Double.longBitsToDouble(seed);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        return value;
    }
}