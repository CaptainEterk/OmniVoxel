package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;

@Function(id = "y")
public class YDensityFunction extends DensityFunction {
    public YDensityFunction(GameNode args, long seed) {
        super(args, seed);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        return y;
    }
}
