package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3DProvider;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

import java.util.HashMap;
import java.util.Map;

@Function(id = "weird_scaled_sampler")
public class WeirdScaledSamplerDensityFunction extends DensityFunction {
    public static final Map<String, Noise3D> noises = new HashMap<>();

    private final Noise3D noise;
    private final DensityFunction input;
    private final String rarityValueMapper;

    public WeirdScaledSamplerDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode obj = Game.checkGameNodeType(args, ObjectGameNode.class);

        this.rarityValueMapper = Game.checkGameNodeType(obj.object().get("rarity_value_mapper"), StringGameNode.class).value();
        String noiseId = Game.checkGameNodeType(obj.object().get("noise"), StringGameNode.class).value();
        this.noise = Noise3DProvider.get(noiseId);

        this.input = ServerWorldDataService.getDensityFunction(obj.object().get("input"), seed);
    }

    private double mapScale(double value) {
        // This maps [-1..1] input to a [min..max] scale depending on mapper type
        double min, max;
        if ("type_1".equalsIgnoreCase(rarityValueMapper)) {
            min = 0.75;
            max = 2.0;
        } else if ("type_2".equalsIgnoreCase(rarityValueMapper)) {
            min = 0.5;
            max = 3.0;
        } else {
            throw new IllegalArgumentException("Unknown rarity_value_mapper: " + rarityValueMapper);
        }
        // Normalize input [-1,1] -> [0,1]
        double t = (value + 1.0) / 2.0;
        return min + (max - min) * t;
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double inputVal = input.evaluate(x, y, z);
        double scale = mapScale(inputVal);

        double nx = x * scale;
        double ny = y * scale;
        double nz = z * scale;

        return Math.abs(noise.generate(nx, ny, nz));
    }
}
