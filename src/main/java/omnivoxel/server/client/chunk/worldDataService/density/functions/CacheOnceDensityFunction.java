package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "cache_once")
public class CacheOnceDensityFunction extends DensityFunction {

    private final DensityFunction argument;
//    private final Map<Long, Double> cache = new HashMap<>();

    public CacheOnceDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.argument = ServerWorldDataService.getDensityFunction(
                    objectGameNode.object().get("arg"), seed);
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    private long key(int x, int y, int z) {
        // pack 3 ints into one long (x and z in 21 bits each, y in 22 bits) — enough for typical world ranges
        return (((long) x & 0x1FFFFF) << 43) | (((long) y & 0x3FFFFF) << 21) | ((long) z & 0x1FFFFF);
    }

    @Override
    public double evaluate(double x, double y, double z) {
//        int xi = (int) Math.floor(x);
//        int yi = (int) Math.floor(y);
//        int zi = (int) Math.floor(z);
//
//        long key = key(xi, yi, zi);

        return argument.evaluate(x, y, z);
//        Double cached = cache.get(key);
//        if (cached == null) {
//            cached = argument.evaluate(xi, yi, zi);
//            cache.put(key, cached);
//        }
//        return cached;
    }
}
