package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

import java.util.Random;

@Function(id = "old_blended_noise")
public class OldBlendedNoiseDensityFunction extends DensityFunction {

    private final double xzScale;
    private final double yScale;
    private final double xzFactor;
    private final double yFactor;
    private final double smearScaleMultiplier;

    private final Noise3D mainNoise;
    private final Noise3D minLimitNoise;
    private final Noise3D maxLimitNoise;

    public OldBlendedNoiseDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode obj = (ObjectGameNode) args;

        this.xzScale = getDouble(obj, "xz_scale");
        this.yScale = getDouble(obj, "y_scale");
        this.xzFactor = getDouble(obj, "xz_factor");
        this.yFactor = getDouble(obj, "y_factor");
        this.smearScaleMultiplier = getDouble(obj, "smear_scale_multiplier");

        Random random = new Random(seed);

        // MC uses ~16 octaves, firstOctave = -7
        double[] amplitudes = new double[16];
        for (int i = 0; i < 16; i++) amplitudes[i] = 1.0 / (1 << i);

        this.mainNoise = new Noise3D(amplitudes, -7, random.nextLong());
        this.minLimitNoise = new Noise3D(amplitudes, -7, random.nextLong());
        this.maxLimitNoise = new Noise3D(amplitudes, -7, random.nextLong());
    }

    private static double getDouble(ObjectGameNode obj, String key) {
        Object val = obj.object().get(key);
        if (val instanceof DoubleGameNode d) return d.value();
        throw new IllegalArgumentException("Expected double for key " + key);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double nx = x * xzScale;
        double ny = y * yScale;
        double nz = z * xzScale;

        double main = mainNoise.generate(nx, ny, nz);
        double minLimit = minLimitNoise.generate(nx, ny, nz);
        double maxLimit = maxLimitNoise.generate(nx, ny, nz);

        double blend = (main + 1.0) * 0.5; // -1..1 -> 0..1
        double blended = lerp(blend, minLimit, maxLimit);

        return blended * yFactor + smearScaleMultiplier * xzFactor;
    }
}