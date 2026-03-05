package omnivoxel.server.client.chunk.worldDataService.noise;

public class QuickNoise3D {
    private final long seed;

    public QuickNoise3D(long seed) {
        this.seed = seed;
    }

    private static long hash3D(int x, int y, int z, long seed) {
        long h = seed;
        h ^= 0x9E3779B97F4A7C15L * x;
        h ^= 0xC2B2AE3D27D4EB4FL * y;
        h ^= 0x165667B19E3779F9L * z;
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 32);
        return h;
    }

    public double generate(double x, double y, double z) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);

        long h = hash3D(xi, yi, zi, seed);

        return (h / (double) Long.MAX_VALUE);
    }
}