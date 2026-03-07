package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "half_negative")
public class HalfNegativeDensityFunction extends DensityFunction {
    private final DensityFunction input;

    public HalfNegativeDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode object = Game.checkGameNodeType(args, ObjectGameNode.class);
        this.input = ServerWorldDataService.getDensityFunction(object.object().get("arg"), seed);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double v = input.evaluate(x, y, z);
        return v < 0 ? v / 2.0 : v;
    }
}