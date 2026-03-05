package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "range_choice")
public class RangeChoiceDensityFunction extends DensityFunction {
    private final DensityFunction input;
    private final DensityFunction minInclusive;
    private final DensityFunction maxExclusive;
    private final DensityFunction whenInRange;
    private final DensityFunction whenOutOfRange;

    public RangeChoiceDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.input = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("input"), seed);
            this.minInclusive = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("min_inclusive"), seed);
            this.maxExclusive = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("max_exclusive"), seed);
            this.whenInRange = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("in_range"), seed);
            this.whenOutOfRange = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("out_range"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double v = input.evaluate(x, y, z);
        if (v >= minInclusive.evaluate(x, y, z) && v < maxExclusive.evaluate(x, y, z)) {
            return whenInRange.evaluate(x, y, z);
        }
        return whenOutOfRange.evaluate(x, y, z);
    }
}