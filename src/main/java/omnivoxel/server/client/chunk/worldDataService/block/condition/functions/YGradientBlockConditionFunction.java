package omnivoxel.server.client.chunk.worldDataService.block.condition.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.condition.BlockConditionFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.QuickNoise3D;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "y_gradient")
public class YGradientBlockConditionFunction extends BlockConditionFunction {
    private final QuickNoise3D noise;
    private final double low;
    private final double high;

    public YGradientBlockConditionFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);

        this.noise = new QuickNoise3D(seed);
        this.low = Game.checkGameNodeType(objectGameNode.object().get("low"), DoubleGameNode.class).value();
        this.high = Game.checkGameNodeType(objectGameNode.object().get("high"), DoubleGameNode.class).value();
    }

    @Override
    public boolean evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        if (y <= low) {
            return true;
        } else if (y >= high) {
            return false;
        } else {
            double prob = (high - y) / (high - low);
            double n = (noise.generate(x, y, z) + 1.0) / 2.0;
            return n < prob;
        }
    }
}