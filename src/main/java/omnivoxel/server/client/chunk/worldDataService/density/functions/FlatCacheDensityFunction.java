package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Function(id = "flat_cache")
public class FlatCacheDensityFunction extends DensityFunction {

    private final DensityFunction arg;
    private final Map<Long, Double> cache = new ConcurrentHashMap<>();

    public FlatCacheDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.arg = ServerWorldDataService.getDensityFunction(objectGameNode.object().get("arg"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        long key = (((long) x) << 32) | (((long) z) & 0xFFFFFFFFL);

        return cache.computeIfAbsent(key, k -> arg.evaluate(x, 0, z));
    }
}