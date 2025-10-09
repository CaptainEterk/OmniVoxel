package omnivoxel.server.client.chunk.worldDataService.noise;

public class Noise3DTest {

    public static void main(String[] args) {
        // Define a simple octave configuration
        double[] amplitudes = {1.0, 0.5, 0.25, 0.125};
        double firstOctave = -2;
        long seed = 12345L;

        Noise3D noise = new Noise3D(amplitudes, firstOctave, seed);

        // --- Test 1: Basic consistency ---
        double val1 = noise.generate(10.5, 20.25, 30.75);
        double val2 = noise.generate(10.5, 20.25, 30.75);
        System.out.println("Consistency test:");
        System.out.println("  First value : " + val1);
        System.out.println("  Second value: " + val2);
        System.out.println("  Equal? " + (val1 == val2));
        System.out.println();

        // --- Test 2: Small spatial changes ---
        System.out.println("Spatial variation test:");
        for (int i = 0; i < 5; i++) {
            double x = 10.0 + i * 0.1;
            double y = 20.0 + i * 0.1;
            double z = 30.0 + i * 0.1;
            double v = noise.generate(x, y, z);
            System.out.printf("  (%.2f, %.2f, %.2f) -> %.6f%n", x, y, z, v);
        }
        System.out.println();

        // --- Test 3: Performance with caching ---
        System.out.println("Performance test (cache warmup + re-run):");

        int sampleCount = 200_000;
        double total = 0.0;

        // Warm-up phase (fills cache)
        long start = System.nanoTime();
        for (int i = 0; i < sampleCount; i++) {
            total += noise.generate(i * 0.01, i * 0.01, i * 0.01);
        }
        long warmupTime = System.nanoTime() - start;

        // Cached re-run
        start = System.nanoTime();
        for (int i = 0; i < sampleCount; i++) {
            total += noise.generate(i * 0.01, i * 0.01, i * 0.01);
        }
        long cachedTime = System.nanoTime() - start;

        System.out.printf("  Warmup (uncached): %.2f ms%n", warmupTime / 1e6);
        System.out.printf("  Cached re-run:     %.2f ms%n", cachedTime / 1e6);
        System.out.printf("  Speedup: %.2fx%n", (double) warmupTime / cachedTime);
        System.out.println();

        // --- Test 4: Edge case sampling ---
        System.out.println("Edge case test:");
        System.out.println("  Large coords: " + noise.generate(1e6, 2e6, 3e6));
        System.out.println("  Negative coords: " + noise.generate(-15.5, -20.75, -5.25));
        System.out.println("  Zero coords: " + noise.generate(0, 0, 0));
    }
}
