package edu.cnu.mdi.optics.model;

/** Headings and segments. Headings are degrees clockwise from up, as MDI uses. */
public final class Geometry {

    private Geometry() {
    }

    /** x component of the unit vector pointing along a heading. */
    public static double dirX(double heading) {
        return Math.sin(Math.toRadians(heading));
    }

    /** y component of the unit vector pointing along a heading. */
    public static double dirY(double heading) {
        return Math.cos(Math.toRadians(heading));
    }

    /**
     * Distance from a point to a segment given by its center, the heading its
     * face points (the segment runs at right angles to this) and its length.
     */
    public static double distanceToSegment(double px, double py, double cx, double cy, double heading,
            double length) {
        double tx = dirY(heading);
        double ty = -dirX(heading);
        double along = Math.max(-length / 2, Math.min(length / 2, (px - cx) * tx + (py - cy) * ty));
        return Math.hypot(px - cx - along * tx, py - cy - along * ty);
    }
}
