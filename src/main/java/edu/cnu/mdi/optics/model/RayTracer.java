package edu.cnu.mdi.optics.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Follows the laser's rays across a level. Mirrors reflect them, splitters
 * divide them into two of half the power, ideal thin lenses bend them, and
 * walls, targets, the laser itself and the edge of the board stop them.
 */
public final class RayTracer {

    /** How close to a target's center a beam must pass to hit it. */
    public static final double TARGET_RADIUS = 0.35;
    /** Distance between neighbouring rays when the laser sends more than one. */
    public static final double RAY_SPACING = 1.0 / 3;
    /** The beam may run this far past the outermost grid points. */
    public static final double MARGIN = 0.5;

    /** Beams weaker than this are dropped. */
    private static final double LASER_RADIUS = 0.35;
    private static final double MIN_POWER = 1.0 / 64;
    /** Stops mirrors that face each other from looping forever. */
    private static final int MAX_SEGMENTS = 200;
    private static final double EPS = 1.0e-9;

    /** A straight stretch of beam. {@code power} is the fraction of the laser's. */
    public record Segment(double x1, double y1, double x2, double y2, double power) {
    }

    /** The paths of the rays, and the total power reaching each target, in level order. */
    public record Result(List<Segment> segments, List<Double> targetPower) {
    }

    private record Ray(double x, double y, double dx, double dy, double power, Piece from) {
    }

    private RayTracer() {
    }

    public static Result trace(Level level, List<Piece> pieces) {
        List<Segment> segments = new ArrayList<>();
        double[] received = new double[level.targets().size()];
        ArrayDeque<Ray> pending = new ArrayDeque<>();
        Level.Laser laser = level.laser();
        double dx0 = Geometry.dirX(laser.heading());
        double dy0 = Geometry.dirY(laser.heading());
        for (int i = 0; i < laser.rays(); i++) {
            // Side by side, centered on the laser, at right angles to the beam.
            double offset = (i - (laser.rays() - 1) / 2.0) * RAY_SPACING;
            pending.add(new Ray(laser.x() + offset * dy0, laser.y() - offset * dx0, dx0, dy0,
                    1.0 / laser.rays(), null));
        }

        while (!pending.isEmpty() && segments.size() < MAX_SEGMENTS) {
            Ray ray = pending.remove();
            double distance = distanceToEdge(ray);
            Piece hitPiece = null;
            int hitTarget = -1;

            for (Level.Wall wall : level.walls()) {
                double s = hitSegment(ray, wall.x(), wall.y(), wall.heading(), wall.length());
                if (s < distance) {
                    distance = s;
                    hitPiece = null;
                    hitTarget = -1;
                }
            }
            // The beam starts inside the laser, so this only stops light coming back.
            double back = hitCircle(ray, laser.x(), laser.y(), LASER_RADIUS);
            if (back < distance) {
                distance = back;
                hitPiece = null;
            }
            for (int i = 0; i < level.targets().size(); i++) {
                Level.Target target = level.targets().get(i);
                double s = hitCircle(ray, target.x(), target.y(), target.radius());
                if (s < distance) {
                    distance = s;
                    hitPiece = null;
                    hitTarget = i;
                }
            }
            for (Piece piece : pieces) {
                if (piece == ray.from()) {
                    continue;
                }
                double s = hitSegment(ray, piece.x(), piece.y(), piece.heading(), Piece.LENGTH);
                if (s < distance) {
                    distance = s;
                    hitPiece = piece;
                    hitTarget = -1;
                }
            }

            double x = ray.x() + distance * ray.dx();
            double y = ray.y() + distance * ray.dy();
            segments.add(new Segment(ray.x(), ray.y(), x, y, ray.power()));

            if (hitTarget >= 0) {
                received[hitTarget] += ray.power();
            } else if (hitPiece != null && hitPiece.kind().isLens()) {
                pending.add(refract(ray, hitPiece, level.focalLength(), x, y));
            } else if (hitPiece != null) {
                double nx = Geometry.dirX(hitPiece.heading());
                double ny = Geometry.dirY(hitPiece.heading());
                double dot = ray.dx() * nx + ray.dy() * ny;
                boolean splits = hitPiece.kind() == Kind.SPLITTER;
                double power = splits ? ray.power() / 2 : ray.power();
                if (power >= MIN_POWER) {
                    pending.add(new Ray(x, y, ray.dx() - 2 * dot * nx, ray.dy() - 2 * dot * ny, power, hitPiece));
                    if (splits) {
                        pending.add(new Ray(x, y, ray.dx(), ray.dy(), power, hitPiece));
                    }
                }
            }
        }

        List<Double> targetPower = new ArrayList<>();
        for (double power : received) {
            targetPower.add(power);
        }
        return new Result(List.copyOf(segments), List.copyOf(targetPower));
    }

    /**
     * The ray leaving an ideal thin lens. Measured against the lens axis, its
     * slope drops by h/f, where h is how far from the center it struck.
     */
    private static Ray refract(Ray ray, Piece lens, double focalLength, double x, double y) {
        double nx = Geometry.dirX(lens.heading());
        double ny = Geometry.dirY(lens.heading());
        if (ray.dx() * nx + ray.dy() * ny < 0) {
            nx = -nx; // the axis points the way the light is going
            ny = -ny;
        }
        double tx = -ny;
        double ty = nx;
        double f = lens.kind() == Kind.CONVERGING ? focalLength : -focalLength;
        double along = ray.dx() * nx + ray.dy() * ny;
        double across = ray.dx() * tx + ray.dy() * ty;
        double h = (x - lens.x()) * tx + (y - lens.y()) * ty;
        double slope = across / along - h / f;
        double norm = Math.hypot(1, slope);
        return new Ray(x, y, (nx + slope * tx) / norm, (ny + slope * ty) / norm, ray.power(), lens);
    }

    private static double distanceToEdge(Ray ray) {
        double sx = Double.POSITIVE_INFINITY;
        double sy = Double.POSITIVE_INFINITY;
        if (ray.dx() > EPS) {
            sx = (Level.WIDTH + MARGIN - ray.x()) / ray.dx();
        } else if (ray.dx() < -EPS) {
            sx = (-MARGIN - ray.x()) / ray.dx();
        }
        if (ray.dy() > EPS) {
            sy = (Level.HEIGHT + MARGIN - ray.y()) / ray.dy();
        } else if (ray.dy() < -EPS) {
            sy = (-MARGIN - ray.y()) / ray.dy();
        }
        return Math.max(0, Math.min(sx, sy));
    }

    /** Distance along the ray to the segment, or infinity if it misses. */
    private static double hitSegment(Ray ray, double cx, double cy, double heading, double length) {
        double tx = Geometry.dirY(heading);
        double ty = -Geometry.dirX(heading);
        double denominator = ray.dx() * ty - ray.dy() * tx;
        if (Math.abs(denominator) < EPS) {
            return Double.POSITIVE_INFINITY; // edge on
        }
        double ox = cx - ray.x();
        double oy = cy - ray.y();
        double s = (ox * ty - oy * tx) / denominator;
        double u = (ox * ray.dy() - oy * ray.dx()) / denominator;
        return s > EPS && Math.abs(u) <= length / 2 + EPS ? s : Double.POSITIVE_INFINITY;
    }

    /** Distance along the ray to where it enters the circle, or infinity. */
    private static double hitCircle(Ray ray, double cx, double cy, double radius) {
        double ox = cx - ray.x();
        double oy = cy - ray.y();
        double middle = ox * ray.dx() + oy * ray.dy();
        double missSquared = ox * ox + oy * oy - middle * middle;
        if (missSquared > radius * radius) {
            return Double.POSITIVE_INFINITY;
        }
        double s = middle - Math.sqrt(radius * radius - missSquared);
        return s > EPS ? s : Double.POSITIVE_INFINITY;
    }
}
