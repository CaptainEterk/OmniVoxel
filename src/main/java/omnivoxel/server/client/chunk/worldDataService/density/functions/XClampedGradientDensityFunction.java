package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "x_clamped_gradient")
public class XClampedGradientDensityFunction extends DensityFunction {
    private final DensityFunction from;
    private final DensityFunction to;
    private final DensityFunction fromValue;
    private final DensityFunction toValue;

    public XClampedGradientDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.from = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg"), seed);
            this.to = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg"), seed);
            this.fromValue = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg"), seed);
            this.toValue = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double from = this.from.evaluate(x, y, z);
        double to = this.to.evaluate(x, y, z);
        double fromVal = fromValue.evaluate(x, y, z);
        double toVal = toValue.evaluate(x, y, z);

        return from == to ? fromVal : fromVal + (Math.max(from, Math.min(x, to)) - from) / (to - from) * (toVal - fromVal);
    }
}