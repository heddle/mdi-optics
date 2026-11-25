package edu.cnu.mdi.optics.model;

import java.util.List;

/**
 * One puzzle: the fixed scenery and the pieces the player is given.
 *
 * @param name      shown as the level's title
 * @param hint      one or two sentences of help
 * @param laser     the beam source
 * @param targets   what has to be lit
 * @param walls     obstacles that stop the beam
 * @param mirrors   how many mirrors the player may place
 * @param splitters how many splitters the player may place
 */
public record Level(String name, String hint, Laser laser, List<Target> targets, List<Wall> walls,
        int mirrors, int splitters) {

    /** Grid columns run from 0 to this value. */
    public static final int WIDTH = 16;
    /** Grid rows run from 0 to this value. */
    public static final int HEIGHT = 12;

    /** The beam source. {@code heading} is in degrees clockwise from up. */
    public record Laser(int x, int y, double heading) {
    }

    /** A detector that is lit when at least {@code power} of the beam reaches it. */
    public record Target(int x, int y, double power) {
    }

    /** A straight obstacle centered on ({@code x}, {@code y}), facing {@code heading}. */
    public record Wall(int x, int y, double heading, double length) {
    }

    public Level {
        if (name == null || laser == null || targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException("A level needs a name, a laser and a target");
        }
        hint = hint == null ? "" : hint;
        targets = List.copyOf(targets);
        walls = walls == null ? List.of() : List.copyOf(walls);
        if (mirrors < 0 || splitters < 0) {
            throw new IllegalArgumentException("Piece counts cannot be negative");
        }
    }

    /** How many pieces of a kind the player may have on the board. */
    public int allowed(Kind kind) {
        return kind == Kind.MIRROR ? mirrors : splitters;
    }

    /** Whether a piece may be put on this grid point: on the board and clear of scenery. */
    public boolean isOpen(int x, int y) {
        if (x < 0 || x > WIDTH || y < 0 || y > HEIGHT || (x == laser.x() && y == laser.y())) {
            return false;
        }
        for (Target target : targets) {
            if (x == target.x() && y == target.y()) {
                return false;
            }
        }
        for (Wall wall : walls) {
            if (Geometry.distanceToSegment(x, y, wall.x(), wall.y(), wall.heading(), wall.length()) < 0.1) {
                return false;
            }
        }
        return true;
    }

    /** Whether the given per-target powers light every target. */
    public boolean isSolvedBy(RayTracer.Result result) {
        for (int i = 0; i < targets.size(); i++) {
            if (!isLit(i, result)) {
                return false;
            }
        }
        return true;
    }

    /** Whether target {@code index} receives the power it needs. */
    public boolean isLit(int index, RayTracer.Result result) {
        return result.targetPower().get(index) >= targets.get(index).power() - 1.0e-9;
    }
}
