package omnivoxel.server.client.chunk.worldDataService.density.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.ArrayGameNode;
import omnivoxel.util.game.nodes.DoubleGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

import java.util.ArrayList;
import java.util.List;

@Function(id = "spline")
public class SplineDensityFunction extends DensityFunction {

    private final DensityFunction coordinate;
    private final List<Point> points = new ArrayList<>();

    public SplineDensityFunction(GameNode args, long seed) {
        super(args, seed);

        if (!(args instanceof ObjectGameNode objectGameNode))
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode");

        var obj = objectGameNode.object();
        this.coordinate = ServerWorldDataService.getDensityFunction(obj.get("coordinate"), seed);

        GameNode[] pts = Game.checkGameNodeType(objectGameNode.object().get("points"), ArrayGameNode.class).nodes();
        if (pts == null || pts.length == 0)
            throw new IllegalArgumentException("spline must have at least one point");

        for (GameNode node : pts) {
            ObjectGameNode o = Game.checkGameNodeType(node, ObjectGameNode.class);
            double loc = (Game.checkGameNodeType(o.object().get("location"), DoubleGameNode.class)).value();
            double der = (Game.checkGameNodeType(o.object().get("derivative"), DoubleGameNode.class)).value();

            if (o.object().get("value") instanceof ObjectGameNode objectGameNode1 && objectGameNode1.object().get("type") == null) {
                System.out.println(objectGameNode1.object());
            }

            DensityFunction valueFn = ServerWorldDataService.getDensityFunction(o.object().get("value"), seed);

            points.add(new Point(loc, valueFn, der));
        }
    }

    @Override
    public double evaluate(double x, double y, double z) {
        double coord = coordinate.evaluate(x, y, z);
        return evaluateSpline(coord, x, y, z);
    }

    private double evaluateSpline(double coord, double x, double y, double z) {
        if (points.size() == 1)
            return points.getFirst().value.evaluate(x, y, z);

        // Clamp coord between first and last location
        if (coord <= points.getFirst().location)
            return points.getFirst().value.evaluate(x, y, z);
        if (coord >= points.getLast().location)
            return points.getLast().value.evaluate(x, y, z);

        // Find interval
        for (int i = 0; i < points.size() - 1; i++) {
            Point p0 = points.get(i);
            Point p1 = points.get(i + 1);

            if (coord >= p0.location && coord <= p1.location) {
                double v0 = p0.value.evaluate(x, y, z);
                double v1 = p1.value.evaluate(x, y, z);
                return interpolateHermite(coord, p0.location, v0, p0.derivative,
                        p1.location, v1, p1.derivative);
            }
        }
        // Should not reach here
        return 0.0;
    }

    private double interpolateHermite(double x, double x0, double v0, double m0,
                                      double x1, double v1, double m1) {
        double t = (x - x0) / (x1 - x0);
        double t2 = t * t;
        double t3 = t2 * t;

        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;

        return h00 * v0 + h10 * (x1 - x0) * m0 + h01 * v1 + h11 * (x1 - x0) * m1;
    }

    private record Point(double location, DensityFunction value, double derivative) {
    }
}
