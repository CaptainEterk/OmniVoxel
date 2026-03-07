package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "clamp")
public class ClampDensityFunction extends DensityFunction {
    private final DensityFunction input;
    private final double min;
    private final double max;

    public ClampDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode obj = (ObjectGameNode) args;
        this.input = ServerWorldDataService.getDensityFunction(obj.object().get("input"), seed);

        this.min = getDouble(obj, "min");
        this.max = getDouble(obj, "max");
    }

    private static double getDouble(ObjectGameNode obj, String key) {
        Object val = obj.object().get(key);
        if (val instanceof DoubleGameNode d) return d.value();
        throw new IllegalArgumentException("Expected double for key " + key);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double v = input.evaluate(x, y, z);
        return Math.max(min, Math.min(max, v));
    }
}
