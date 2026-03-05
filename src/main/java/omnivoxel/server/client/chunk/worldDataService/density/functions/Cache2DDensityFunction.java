package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

import java.util.HashMap;
import java.util.Map;

@Function(id = "cache_2d")
public class Cache2DDensityFunction extends DensityFunction {

    private final DensityFunction argument;
    private final Map<Long, Double> cache = new HashMap<>();

    public Cache2DDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.argument = ServerWorldDataService.getDensityFunction(
                    objectGameNode.object().get("arg"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);

        long key = (((long) xi) << 32) | ((long) zi & 0xFFFFFFFFL);

        Double cached = cache.get(key);
        if (cached == null) {
            cached = argument.evaluate(xi, 0, zi);
            cache.put(key, cached);
        }
        return cached;
    }
}