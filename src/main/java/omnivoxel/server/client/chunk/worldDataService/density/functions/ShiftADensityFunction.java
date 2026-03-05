package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3DProvider;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

@Function(id = "shift_a")
public class ShiftADensityFunction extends DensityFunction {
    private final Noise3D noise;

    public ShiftADensityFunction(GameNode args, long seed) {
        super(args, seed);
        ObjectGameNode object = Game.checkGameNodeType(args, ObjectGameNode.class);
        StringGameNode noiseNode = Game.checkGameNodeType(object.object().get("arg"), StringGameNode.class);
        this.noise = Noise3DProvider.get(noiseNode.value());
    }

    @Override
    public double evaluate(double x, double y, double z) {
        return noise.generate(x / 4.0, 0.0, z / 4.0) * 4.0;
    }
}
