package omnivoxel.util.cache;

public class NoiseCache {
    private static final int SIZE = 3;

    private final int[] xs = new int[SIZE];
    private final int[] ys = new int[SIZE];
    private final int[] zs = new int[SIZE];
    private final double[] values = new double[SIZE];
    private final boolean[] valid = new boolean[SIZE];

    private int next = 0;

    public Double getCached(int x, int y, int z) {
        for (int i = 0; i < SIZE; i++) {
            if (valid[i] && xs[i] == x && ys[i] == y && zs[i] == z) {
                return values[i];
            }
        }
        return null;
    }

    public void cache(int x, int y, int z, double value) {
        xs[next] = x;
        ys[next] = y;
        zs[next] = z;
        values[next] = value;
        valid[next] = true;
        next = (next + 1) % SIZE;
    }
}
