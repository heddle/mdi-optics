package edu.cnu.mdi.optics.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
