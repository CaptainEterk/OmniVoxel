package omnivoxel.server.client.chunk.worldDataService.noise;

public class MinecraftOctaveNoise {
    private final PerlinNoise[] octaves;
    private final double[] amplitudes;
    private final int octaveCount;

    public MinecraftOctaveNoise(int seed, int octaveCount) {
        this.octaveCount = octaveCount;
        this.octaves = new PerlinNoise[octaveCount];
        this.amplitudes = new double[octaveCount];

        for (int i = 0; i < octaveCount; i++) {
            octaves[i] = new PerlinNoise(seed + i * 31L);
            amplitudes[i] = 1.0 / (1 << i); // halves each octave
        }
    }

    public double sample(double x, double y, double z) {
        double result = 0.0;
        double frequency = 1.0;

        for (int i = 0; i < octaveCount; i++) {
            result += octaves[i].sample(x * frequency, y * frequency, z * frequency) * amplitudes[i];
            frequency *= 2.0;
        }

        return result;
    }
}
