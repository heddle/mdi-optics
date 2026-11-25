package edu.cnu.mdi.optics.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import edu.cnu.mdi.optics.model.Kind;

/** Draws mirrors and splitters, so the palette and the board always match. */
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
        if (kind == Kind.MIRROR) {
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
}
