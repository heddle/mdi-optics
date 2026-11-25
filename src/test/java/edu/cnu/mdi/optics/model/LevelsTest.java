package edu.cnu.mdi.optics.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LevelsTest {

    private final List<Level> levels = Levels.load();

    @Test
    void thereAreNineLevels() {
        assertEquals(9, levels.size());
    }

    @Test
    void sceneryDoesNotOverlap() {
        for (Level level : levels) {
            Level.Laser laser = level.laser();
            for (Level.Wall wall : level.walls()) {
                assertTrue(Geometry.distanceToSegment(laser.x(), laser.y(), wall.x(), wall.y(),
                        wall.heading(), wall.length()) > 0.5, level.name() + ": wall on laser");
                for (Level.Target target : level.targets()) {
                    assertTrue(Geometry.distanceToSegment(target.x(), target.y(), wall.x(), wall.y(),
                            wall.heading(), wall.length()) > 0.5, level.name() + ": wall on target");
                }
            }
        }
    }

    /**
     * Beams cross a row or column only at grid points, so a gap one square wide
     * looks open but cannot be passed: both its edges are wall.
     */
    @Test
    void noGapInALineOfWallsIsTooNarrowToPass() {
        for (Level level : levels) {
            for (Level.Wall a : level.walls()) {
                for (Level.Wall b : level.walls()) {
                    double tx = Geometry.dirY(a.heading());
                    double ty = -Geometry.dirX(a.heading());
                    double dx = b.x() - a.x();
                    double dy = b.y() - a.y();
                    boolean inLine = a != b && (a.heading() - b.heading()) % 180 == 0
                            && Math.abs(dx * ty - dy * tx) < 1.0e-9;
                    double gap = Math.abs(dx * tx + dy * ty) - (a.length() + b.length()) / 2;
                    assertFalse(inLine && gap > 1.0e-9 && gap < 2 - 1.0e-9,
                            level.name() + ": gap of " + gap + " between " + a + " and " + b);
                }
            }
        }
    }

    @Test
    void noLevelStartsSolved() {
        for (Level level : levels) {
            assertNull(Solver.solve(level, 0), level.name());
        }
    }

    @Test
    void everyLevelCanBeSolvedWithItsPieces() {
        assertEquals(levels.size(), Solutions.ALL.size());
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            List<Piece> solution = Solutions.ALL.get(i);
            for (Kind kind : Kind.values()) {
                assertTrue(solution.stream().filter(p -> p.kind() == kind).count() <= level.allowed(kind),
                        level.name() + " uses too many " + kind);
            }
            assertTrue(solution.stream().allMatch(p -> level.isOpen((int) p.x(), (int) p.y())), level.name());
            assertTrue(level.isSolvedBy(RayTracer.trace(level, solution)), level.name());
        }
    }

    @Test
    void everyLevelNeedsAllItsPieces() {
        for (Level level : levels) {
            assertNull(Solver.solve(level, level.pieceCount() - 1), level.name());
        }
    }
}
