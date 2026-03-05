package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3DProvider;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

@Function(id = "noise3d")
public class Noise3DDensityFunction extends DensityFunction {
    private final Noise3D noise;
    private final double xScale, yScale, zScale;

    public Noise3DDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);

        StringGameNode stringGameNode =
                Game.checkGameNodeType(objectGameNode.object().get("noise_id"), StringGameNode.class);
        this.noise = Noise3DProvider.get(stringGameNode.value());

        this.xScale = getScale(objectGameNode, "x_scale");
        this.yScale = getScale(objectGameNode, "y_scale");
        this.zScale = getScale(objectGameNode, "z_scale");
    }

    private static double getScale(ObjectGameNode node, String key) {
        GameNode g = node.object().get(key);
        if (g == null) return 1.0; // default if not present
        return Game.checkGameNodeType(g, DoubleGameNode.class).value();
    }

    @Override
    public double evaluate(double x, double y, double z) {
        return noise.generate(x * xScale, y * yScale, z * zScale);
    }
}