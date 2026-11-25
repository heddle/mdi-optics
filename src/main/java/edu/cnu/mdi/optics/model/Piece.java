package edu.cnu.mdi.optics.model;

import java.util.Objects;

/**
 * A mirror or splitter on the board.
 *
 * @param kind    what the piece does to a beam
 * @param x       center, in board units
 * @param y       center, in board units, increasing upward
 * @param heading direction the face points, in degrees clockwise from up
 */
public record Piece(Kind kind, double x, double y, double heading) {

    /** Length of every mirror and splitter, in board units. */
    public static final double LENGTH = 1.2;

    public Piece {
        Objects.requireNonNull(kind, "kind");
    }
}
