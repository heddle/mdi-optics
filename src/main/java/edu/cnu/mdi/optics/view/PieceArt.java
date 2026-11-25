package edu.cnu.mdi.optics.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import edu.cnu.mdi.optics.model.Kind;

/** Draws the pieces, so the palette and the board always match. */
final class PieceArt {

    private static final Color SILVER = new Color(222, 232, 245);
    private static final Color BACKING = new Color(112, 122, 142);
    private static final Color GLASS = new Color(110, 215, 235);

    private PieceArt() {
    }

    /**
     * Draws a piece whose face runs from {@code a} to {@code b}.
     *
     * @param width thickness of the face in pixels
     */
    static void draw(Graphics2D g, Kind kind, Point2D a, Point2D b, float width) {
        Line2D face = new Line2D.Double(a, b);
        if (kind.isLens()) {
            drawLens(g, kind, a, b, width);
        } else if (kind == Kind.MIRROR) {
            g.setStroke(new BasicStroke(width * 1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(BACKING);
            g.draw(face);
            g.setStroke(new BasicStroke(width * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(SILVER);
            g.draw(face);
        } else {
            g.setStroke(new BasicStroke(width * 1.9f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(GLASS.getRed(), GLASS.getGreen(), GLASS.getBlue(), 80));
            g.draw(face);
            g.setStroke(new BasicStroke(width * 0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                    new float[] {width * 1.6f, width}, 0f));
            g.setColor(GLASS);
            g.draw(face);
        }
    }

    /** A lens in section: fat in the middle if it converges, fat at the ends if it diverges. */
    private static void drawLens(Graphics2D g, Kind kind, Point2D a, Point2D b, float width) {
        double mx = (a.getX() + b.getX()) / 2;
        double my = (a.getY() + b.getY()) / 2;
        double length = a.distance(b);
        // Unit vector across the face.
        double nx = -(b.getY() - a.getY()) / length;
        double ny = (b.getX() - a.getX()) / length;
        double half = width * 1.7;

        Path2D.Double shape = new Path2D.Double();
        if (kind == Kind.CONVERGING) {
            shape.moveTo(a.getX(), a.getY());
            shape.quadTo(mx + 2 * half * nx, my + 2 * half * ny, b.getX(), b.getY());
            shape.quadTo(mx - 2 * half * nx, my - 2 * half * ny, a.getX(), a.getY());
        } else {
            double thin = width * 0.35;
            double waist = 2 * thin - half; // control point that puts the middle at the thin value
            shape.moveTo(a.getX() + half * nx, a.getY() + half * ny);
            shape.quadTo(mx + waist * nx, my + waist * ny, b.getX() + half * nx, b.getY() + half * ny);
            shape.lineTo(b.getX() - half * nx, b.getY() - half * ny);
            shape.quadTo(mx - waist * nx, my - waist * ny, a.getX() - half * nx, a.getY() - half * ny);
        }
        shape.closePath();
        g.setColor(new Color(GLASS.getRed(), GLASS.getGreen(), GLASS.getBlue(), 90));
        g.fill(shape);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(GLASS);
        g.draw(shape);
    }
}
