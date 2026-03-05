package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "y_clamped_gradient")
public class YClampedGradientDensityFunction extends DensityFunction {
    private final DensityFunction from;
    private final DensityFunction to;
    private final DensityFunction fromValue;
    private final DensityFunction toValue;

    public YClampedGradientDensityFunction(GameNode node, long seed) {
        super(node, seed);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(node, ObjectGameNode.class);
        try {
            this.from = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("low"), seed);
            this.to = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("high"), seed);
            this.fromValue = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("low_value"), seed);
            this.toValue = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("high_value"), seed);
        } catch (RuntimeException e) {
            System.out.println("SDF " + node);
            throw new RuntimeException(e);
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double from = this.from.evaluate(x, y, z);
        double to = this.to.evaluate(x, y, z);
        double fromVal = fromValue.evaluate(x, y, z);
        double toVal = toValue.evaluate(x, y, z);

        return from == to ? fromVal : fromVal + (Math.max(from, Math.min(y, to)) - from) / (to - from) * (toVal - fromVal);
    }
}