package omnivoxel.server.client.chunk.worldDataService.noise;

import java.util.Random;

public class Noise3D {
    private final PerlinNoise[] octaveNoises;
    private final double[] amplitudes;
    private final int firstOctave;

    public Noise3D(double[] amplitudes, double firstOctave, long seed) {
        this.amplitudes = amplitudes;
        this.firstOctave = (int) firstOctave;
        this.octaveNoises = new PerlinNoise[amplitudes.length];

        Random random = new Random(seed);

        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] != 0.0f) {
                this.octaveNoises[i] = new PerlinNoise(random.nextLong());
            }
        }
    }

    public double generate(double x, double y, double z) {
        double result = 0.0;
        double frequency = Math.pow(2.0, firstOctave);

        for (int i = 0; i < amplitudes.length; i++) {
            PerlinNoise perlin = octaveNoises[i];
            if (perlin != null) {
                result += amplitudes[i] * perlin.sample(
                        x * frequency,
                        y * frequency,
                        z * frequency
                );
            }
            frequency *= 2.0;
        }

        return result;
    }
}