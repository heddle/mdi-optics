package edu.cnu.mdi.optics.model;

import static edu.cnu.mdi.optics.model.Kind.CONVERGING;
import static edu.cnu.mdi.optics.model.Kind.DIVERGING;
import static edu.cnu.mdi.optics.model.Kind.MIRROR;
import static edu.cnu.mdi.optics.model.Kind.SPLITTER;

import java.util.List;

/** One known solution to each level, in level order. Spoilers. */
public final class Solutions {

    public static final List<List<Piece>> ALL = List.of(
            List.of(new Piece(MIRROR, 12, 3, -45)),
            List.of(new Piece(MIRROR, 2, 6, 22.5), new Piece(MIRROR, 8, 0, 0)),
            List.of(new Piece(SPLITTER, 8, 4, -45)),
            List.of(new Piece(MIRROR, 13, 2, 135), new Piece(SPLITTER, 13, 9, 112.5),
                    new Piece(MIRROR, 13, 10, 45)),
            List.of(new Piece(SPLITTER, 8, 9, 45), new Piece(MIRROR, 2, 9, 45),
                    new Piece(SPLITTER, 8, 10, -45), new Piece(MIRROR, 14, 10, -45)),
            List.of(new Piece(CONVERGING, 9, 6, 90)),
            List.of(new Piece(DIVERGING, 5, 6, 90)),
            List.of(new Piece(MIRROR, 12, 2, 135), new Piece(CONVERGING, 12, 5, 0)),
            List.of(new Piece(CONVERGING, 6, 4, 90), new Piece(SPLITTER, 8, 4, -45)));

    private Solutions() {
    }
}
