package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "mul")
public class MulDensityFunction extends DensityFunction {
    private final DensityFunction arg1;
    private final DensityFunction arg2;

    public MulDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.arg1 = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg1"), seed);
            this.arg2 = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg2"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        return arg1.evaluate(x, y, z) * arg2.evaluate(x, y, z);
    }
}
