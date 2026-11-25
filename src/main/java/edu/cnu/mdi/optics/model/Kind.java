package edu.cnu.mdi.optics.model;

/** The kinds of piece a player can put on the board. */
public enum Kind {
    /** Reflects the whole beam. */
    MIRROR,
    /** Reflects half the beam and lets half through. */
    SPLITTER,
    /** A thin lens that brings parallel rays together one focal length away. */
    CONVERGING,
    /** A thin lens that spreads parallel rays as if from one focal length behind it. */
    DIVERGING;

    public boolean isLens() {
        return this == CONVERGING || this == DIVERGING;
    }
}
