package omnivoxel.server.client.chunk.worldDataService.noise;

import java.util.Random;

public class PerlinNoise {
    private static final int PERM_SIZE = 512;
    private final int[] permutations;

    public PerlinNoise(long seed) {
        this.permutations = new int[PERM_SIZE];
        Random random = new Random(seed);

        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        for (int i = 0; i < 256; i++) {
            int j = i + random.nextInt(256 - i);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        for (int i = 0; i < PERM_SIZE; i++) {
            permutations[i] = p[i & 255];
        }
    }

    private static int fastFloor(double d) {
        int xi = (int) d;
        return d < xi ? xi - 1 : xi;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) == 0 ? u : -u) +
                ((h & 2) == 0 ? v : -v);
    }

    public double sample(double x, double y, double z) {
        int X = fastFloor(x) & 255;
        int Y = fastFloor(y) & 255;
        int Z = fastFloor(z) & 255;

        x -= fastFloor(x);
        y -= fastFloor(y);
        z -= fastFloor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = permutations[X] + Y;
        int AA = permutations[A] + Z;
        int AB = permutations[A + 1] + Z;
        int B = permutations[X + 1] + Y;
        int BA = permutations[B] + Z;
        int BB = permutations[B + 1] + Z;

        return lerp(w,
                lerp(v,
                        lerp(u, grad(permutations[AA], x, y, z),
                                grad(permutations[BA], x - 1f, y, z)),
                        lerp(u, grad(permutations[AB], x, y - 1f, z),
                                grad(permutations[BB], x - 1f, y - 1f, z))),
                lerp(v,
                        lerp(u, grad(permutations[AA + 1], x, y, z - 1f),
                                grad(permutations[BA + 1], x - 1f, y, z - 1f)),
                        lerp(u, grad(permutations[AB + 1], x, y - 1f, z - 1f),
                                grad(permutations[BB + 1], x - 1f, y - 1f, z - 1f)))
        );
    }
}