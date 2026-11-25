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
 * @param mirrors     how many mirrors the player may place
 * @param splitters   how many splitters the player may place
 * @param converging  how many converging lenses the player may place
 * @param diverging   how many diverging lenses the player may place
 * @param focalLength focal length of every lens in the level, in board units;
 *                    0 means {@value #DEFAULT_FOCAL_LENGTH}
 */
public record Level(String name, String hint, Laser laser, List<Target> targets, List<Wall> walls,
        int mirrors, int splitters, int converging, int diverging, double focalLength) {

    /** Grid columns run from 0 to this value. */
    public static final int WIDTH = 16;
    /** Grid rows run from 0 to this value. */
    public static final int HEIGHT = 12;
    /** Focal length of a lens when a level does not say. */
    public static final int DEFAULT_FOCAL_LENGTH = 4;

    /**
     * The beam source. {@code heading} is in degrees clockwise from up;
     * {@code rays} is the number of parallel rays it sends, 0 meaning one.
     */
    public record Laser(int x, int y, double heading, int rays) {
        public Laser {
            rays = Math.max(1, rays);
        }

        public Laser(int x, int y, double heading) {
            this(x, y, heading, 1);
        }
    }

    /**
     * A detector that is lit when at least {@code power} of the beam reaches
     * within {@code radius} of its center; a radius of 0 means the usual one.
     */
    public record Target(int x, int y, double power, double radius) {
        public Target {
            radius = radius > 0 ? radius : RayTracer.TARGET_RADIUS;
        }

        public Target(int x, int y, double power) {
            this(x, y, power, 0);
        }
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
        if (mirrors < 0 || splitters < 0 || converging < 0 || diverging < 0 || focalLength < 0) {
            throw new IllegalArgumentException("Piece counts and focal length cannot be negative");
        }
        focalLength = focalLength > 0 ? focalLength : DEFAULT_FOCAL_LENGTH;
    }

    /** A level without lenses. */
    public Level(String name, String hint, Laser laser, List<Target> targets, List<Wall> walls,
            int mirrors, int splitters) {
        this(name, hint, laser, targets, walls, mirrors, splitters, 0, 0, 0);
    }

    /** How many pieces of a kind the player may have on the board. */
    public int allowed(Kind kind) {
        return switch (kind) {
            case MIRROR -> mirrors;
            case SPLITTER -> splitters;
            case CONVERGING -> converging;
            case DIVERGING -> diverging;
        };
    }

    /** How many pieces the player is given in all. */
    public int pieceCount() {
        return mirrors + splitters + converging + diverging;
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
