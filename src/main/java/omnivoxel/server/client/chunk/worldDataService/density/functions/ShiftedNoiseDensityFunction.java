package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3DProvider;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

@Function(id = "shifted_noise")
public class ShiftedNoiseDensityFunction extends DensityFunction {
    private final Noise3D noise;
    private final double xzScale;
    private final double yScale;
    private final DensityFunction shiftX;
    private final DensityFunction shiftY;
    private final DensityFunction shiftZ;

    public ShiftedNoiseDensityFunction(GameNode args, long seed) {
        super(args, seed);

        ObjectGameNode object = Game.checkGameNodeType(args, ObjectGameNode.class);

        StringGameNode noiseNode = Game.checkGameNodeType(object.object().get("noise"), StringGameNode.class);
        this.noise = Noise3DProvider.get(noiseNode.value());

        DoubleGameNode xzScaleNode = Game.checkGameNodeType(object.object().get("xz_scale"), DoubleGameNode.class);
        this.xzScale = xzScaleNode.value();

        DoubleGameNode yScaleNode = Game.checkGameNodeType(object.object().get("y_scale"), DoubleGameNode.class);
        this.yScale = yScaleNode.value();

        this.shiftX = ServerWorldDataService.getDensityFunction(object.object().get("shift_x"), seed);
        this.shiftY = ServerWorldDataService.getDensityFunction(object.object().get("shift_y"), seed);
        this.shiftZ = ServerWorldDataService.getDensityFunction(object.object().get("shift_z"), seed);
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double sx = shiftX.evaluate(x, y, z);
        double sy = shiftY.evaluate(x, y, z);
        double sz = shiftZ.evaluate(x, y, z);

        double nx = (x + sx) * xzScale;
        double ny = (y + sy) * yScale;
        double nz = (z + sz) * xzScale;

        return noise.generate(nx, ny, nz);
    }
}