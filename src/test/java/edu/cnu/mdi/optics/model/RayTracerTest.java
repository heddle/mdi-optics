package edu.cnu.mdi.optics.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RayTracerTest {

    private static final double EPS = 1.0e-9;

    /** Laser at (1,3) firing east. */
    private static Level level(List<Level.Target> targets, List<Level.Wall> walls) {
        return new Level("test", "", new Level.Laser(1, 3, 90), targets, walls, 9, 9);
    }

    private static Level.Target target(int x, int y) {
        return new Level.Target(x, y, 1);
    }

    @Test
    void anUnobstructedBeamRunsToTheEdgeOfTheBoard() {
        RayTracer.Result result = RayTracer.trace(level(List.of(target(5, 9)), List.of()), List.of());

        assertEquals(1, result.segments().size());
        assertEquals(Level.WIDTH + RayTracer.MARGIN, result.segments().get(0).x2(), EPS);
        assertEquals(3, result.segments().get(0).y2(), EPS);
        assertEquals(0.0, result.targetPower().get(0), EPS);
    }

    @Test
    void aMirrorTurnsTheBeamOntoATarget() {
        Level level = level(List.of(target(8, 9)), List.of());
        RayTracer.Result result = RayTracer.trace(level, List.of(new Piece(Kind.MIRROR, 8, 3, -45)));

        RayTracer.Segment turned = result.segments().get(1);
        assertEquals(8, turned.x2(), EPS);
        assertEquals(9 - RayTracer.TARGET_RADIUS, turned.y2(), EPS);
        assertTrue(level.isSolvedBy(result));
    }

    @Test
    void mirrorsWorkFromEitherSide() {
        Level level = level(List.of(target(8, 9)), List.of());

        assertTrue(level.isSolvedBy(RayTracer.trace(level, List.of(new Piece(Kind.MIRROR, 8, 3, 135)))));
    }

    @Test
    void aSplitterSendsHalfEachWay() {
        Level level = level(List.of(new Level.Target(12, 3, 0.5), new Level.Target(8, 9, 0.5)), List.of());
        RayTracer.Result result = RayTracer.trace(level, List.of(new Piece(Kind.SPLITTER, 8, 3, -45)));

        assertEquals(0.5, result.targetPower().get(0), EPS);
        assertEquals(0.5, result.targetPower().get(1), EPS);
        assertTrue(level.isSolvedBy(result));
    }

    @Test
    void halfPowerDoesNotLightATargetThatNeedsAll() {
        Level level = level(List.of(target(12, 3)), List.of());

        assertFalse(level.isSolvedBy(RayTracer.trace(level, List.of(new Piece(Kind.SPLITTER, 8, 3, -45)))));
    }

    @Test
    void aWallStopsTheBeam() {
        Level level = level(List.of(target(12, 3)), List.of(new Level.Wall(6, 3, 90, 2)));
        RayTracer.Result result = RayTracer.trace(level, List.of());

        assertEquals(6, result.segments().get(0).x2(), EPS);
        assertFalse(level.isSolvedBy(result));
    }

    @Test
    void aTargetStopsTheBeam() {
        Level level = level(List.of(target(6, 3), target(12, 3)), List.of());
        RayTracer.Result result = RayTracer.trace(level, List.of());

        assertEquals(1.0, result.targetPower().get(0), EPS);
        assertEquals(0.0, result.targetPower().get(1), EPS);
    }

    @Test
    void theLaserStopsLightSentBackToIt() {
        RayTracer.Result result = RayTracer.trace(level(List.of(target(5, 9)), List.of()),
                List.of(new Piece(Kind.MIRROR, 8, 3, 90)));

        assertEquals(2, result.segments().size());
        assertTrue(result.segments().get(1).x2() > 1, "stopped at the laser, not beyond it");
    }

    @Test
    void facingMirrorsDoNotLoopForever() {
        // The laser sits between two mirrors that bounce the beam back and forth.
        RayTracer.Result result = RayTracer.trace(level(List.of(target(5, 9)), List.of()),
                List.of(new Piece(Kind.MIRROR, 8, 3, 90), new Piece(Kind.MIRROR, 0, 3, 90)));

        assertTrue(result.segments().size() <= 200);
    }

    @Test
    void splittersThatFeedEachOtherDieAway() {
        RayTracer.Result result = RayTracer.trace(level(List.of(target(5, 9)), List.of()),
                List.of(new Piece(Kind.SPLITTER, 8, 3, 90), new Piece(Kind.SPLITTER, 4, 3, 90)));

        assertTrue(result.segments().size() <= 200);
        assertTrue(result.segments().stream().allMatch(segment -> segment.power() >= 1.0 / 64));
    }

    @Test
    void sceneryAndTheEdgeOfTheBoardAreNotOpen() {
        Level level = level(List.of(target(12, 3)), List.of(new Level.Wall(6, 6, 90, 4)));

        assertTrue(level.isOpen(8, 3));
        assertFalse(level.isOpen(1, 3), "laser");
        assertFalse(level.isOpen(12, 3), "target");
        assertFalse(level.isOpen(6, 5), "wall");
        assertTrue(level.isOpen(6, 9), "beyond the wall");
        assertFalse(level.isOpen(17, 3));
        assertFalse(level.isOpen(3, -1));
    }
}
