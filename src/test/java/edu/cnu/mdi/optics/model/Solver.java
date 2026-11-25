package edu.cnu.mdi.optics.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exhaustive search over what a player can build. Pieces sit on grid points and
 * turn in steps of 22.5 degrees, so beams only ever run along grid lines and
 * diagonals, and a piece does something only if it is centered on the beam.
 * Adding pieces in the order the beam reaches them therefore finds every
 * solution.
 */
final class Solver {

    private record Point(int x, int y) {
    }

    private Solver() {
    }

    /** A solution using at most {@code maxPieces} pieces, or {@code null}. */
    static List<Piece> solve(Level level, int maxPieces) {
        return search(level, new ArrayList<>(), maxPieces);
    }

    private static List<Piece> search(Level level, List<Piece> placed, int remaining) {
        RayTracer.Result result = RayTracer.trace(level, placed);
        if (level.isSolvedBy(result)) {
            return List.copyOf(placed);
        }
        if (remaining == 0) {
            return null;
        }
        for (Point point : onBeam(level, result, placed)) {
            for (Kind kind : Kind.values()) {
                if (placed.stream().filter(p -> p.kind() == kind).count() >= level.allowed(kind)) {
                    continue;
                }
                for (int step = 0; step < 8; step++) {
                    placed.add(new Piece(kind, point.x(), point.y(), step * 22.5));
                    List<Piece> found = search(level, placed, remaining - 1);
                    placed.remove(placed.size() - 1);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private static Set<Point> onBeam(Level level, RayTracer.Result result, List<Piece> placed) {
        Set<Point> points = new LinkedHashSet<>();
        for (RayTracer.Segment s : result.segments()) {
            for (int x = 0; x <= Level.WIDTH; x++) {
                for (int y = 0; y <= Level.HEIGHT; y++) {
                    if (Geometry.distanceToSegment(x, y, (s.x1() + s.x2()) / 2, (s.y1() + s.y2()) / 2,
                            Math.toDegrees(Math.atan2(s.x2() - s.x1(), s.y2() - s.y1())) + 90,
                            Math.hypot(s.x2() - s.x1(), s.y2() - s.y1())) < 1.0e-6 && level.isOpen(x, y)) {
                        points.add(new Point(x, y));
                    }
                }
            }
        }
        for (Piece piece : placed) {
            points.remove(new Point((int) piece.x(), (int) piece.y()));
        }
        return points;
    }
}
