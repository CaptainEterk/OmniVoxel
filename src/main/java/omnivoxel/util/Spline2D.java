package omnivoxel.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Spline2D {
    private final List<Point> points = new ArrayList<>();

    public void addPoint(double x, double y) {
        points.add(new Point((x + 1) / 2, (y + 1) / 2));
        points.sort(Comparator.comparingDouble(a -> a.x));
    }

    public double evaluate(double x) {
        if (points.isEmpty()) return 0.0;
        if (points.size() == 1) return points.get(0).y;

        // Clamp if outside bounds
        if (x <= points.get(0).x) return points.get(0).y;
        if (x >= points.get(points.size() - 1).x) return points.get(points.size() - 1).y;

        // Find the two points this x is between
        for (int i = 0; i < points.size() - 1; i++) {
            Point p0 = points.get(i);
            Point p1 = points.get(i + 1);
            if (x >= p0.x && x <= p1.x) {
                double t = (x - p0.x) / (p1.x - p0.x); // Normalize
                return interpolate(p0.y, p1.y, t);
            }
        }

        return 0.0; // Fallback
    }

    private double interpolate(double y0, double y1, double t) {
        // Cubic Hermite (smoothstep-style) interpolation
        t = t * t * (3 - 2 * t); // Smoothstep
        return y0 * (1 - t) + y1 * t;
    }

    public record Point(double x, double y) {
    }
}