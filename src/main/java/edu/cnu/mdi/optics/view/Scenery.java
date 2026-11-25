package edu.cnu.mdi.optics.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.optics.model.Geometry;
import edu.cnu.mdi.optics.model.Level;
import edu.cnu.mdi.optics.model.RayTracer;

/** Draws everything on the board that is not an item: grid, beam, walls, targets and laser. */
final class Scenery {

    private static final Color BOARD = new Color(0x141a28);
    private static final Color OUTSIDE = new Color(0x0a0d15);

    private Scenery() {
    }

    static void draw(Graphics2D original, IContainer container, Level level, RayTracer.Result beam) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(OUTSIDE);
            g.fillRect(0, 0, container.getComponent().getWidth(), container.getComponent().getHeight());

            Point topLeft = pixel(container, -RayTracer.MARGIN, Level.HEIGHT + RayTracer.MARGIN);
            Point bottomRight = pixel(container, Level.WIDTH + RayTracer.MARGIN, -RayTracer.MARGIN);
            g.setColor(BOARD);
            g.fillRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);

            g.setColor(new Color(255, 255, 255, 50));
            for (int x = 0; x <= Level.WIDTH; x++) {
                for (int y = 0; y <= Level.HEIGHT; y++) {
                    Point p = pixel(container, x, y);
                    g.fillOval(p.x - 2, p.y - 2, 4, 4);
                }
            }

            int cell = pixel(container, 1, 0).x - pixel(container, 0, 0).x;
            drawBeam(g, container, beam);
            drawWalls(g, container, level, cell);
            drawTargets(g, container, level, beam, cell);
            drawLaser(g, container, level.laser(), cell);
        } finally {
            g.dispose();
        }
    }

    private static void drawBeam(Graphics2D g, IContainer container, RayTracer.Result beam) {
        for (RayTracer.Segment s : beam.segments()) {
            Line2D line = new Line2D.Double(pixel(container, s.x1(), s.y1()), pixel(container, s.x2(), s.y2()));
            // Weaker beams are fainter and thinner.
            double strength = Math.sqrt(s.power());
            g.setStroke(new BasicStroke((float) (4 + 7 * strength), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 50, 60, (int) (30 + 60 * strength)));
            g.draw(line);
            g.setStroke(new BasicStroke((float) (1 + 2 * strength), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 130, 130, (int) (120 + 135 * strength)));
            g.draw(line);
        }
    }

    private static void drawWalls(Graphics2D g, IContainer container, Level level, int cell) {
        g.setStroke(new BasicStroke(cell * 0.28f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(new Color(0x8b93a7));
        for (Level.Wall wall : level.walls()) {
            double tx = Geometry.dirY(wall.heading()) * wall.length() / 2;
            double ty = -Geometry.dirX(wall.heading()) * wall.length() / 2;
            g.draw(new Line2D.Double(pixel(container, wall.x() - tx, wall.y() - ty),
                    pixel(container, wall.x() + tx, wall.y() + ty)));
        }
    }

    private static void drawTargets(Graphics2D g, IContainer container, Level level, RayTracer.Result beam,
            int cell) {
        int radius = (int) (cell * RayTracer.TARGET_RADIUS);
        for (int i = 0; i < level.targets().size(); i++) {
            Level.Target target = level.targets().get(i);
            boolean lit = level.isLit(i, beam);
            Point p = pixel(container, target.x(), target.y());
            if (lit) {
                g.setColor(new Color(90, 255, 140, 70));
                g.fillOval(p.x - 2 * radius, p.y - 2 * radius, 4 * radius, 4 * radius);
            }
            g.setColor(lit ? new Color(0x5aff8c) : new Color(0x2a3550));
            g.fillOval(p.x - radius, p.y - radius, 2 * radius, 2 * radius);
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(lit ? Color.WHITE : new Color(0xffd24d));
            g.drawOval(p.x - radius, p.y - radius, 2 * radius, 2 * radius);

            // How much of the laser's power this target needs.
            String need = Math.round(target.power() * 100) + "%";
            g.setColor(new Color(0xc8d0e0));
            g.drawString(need, p.x - g.getFontMetrics().stringWidth(need) / 2,
                    p.y + radius + g.getFontMetrics().getAscent() + 1);
        }
    }

    private static void drawLaser(Graphics2D g, IContainer container, Level.Laser laser, int cell) {
        Point p = pixel(container, laser.x(), laser.y());
        Graphics2D rotated = (Graphics2D) g.create();
        try {
            rotated.translate(p.x, p.y);
            // The shape points up; headings run clockwise from up, as screen rotations do.
            rotated.rotate(Math.toRadians(laser.heading()));
            int w = (int) (cell * 0.5);
            int h = (int) (cell * 0.8);
            rotated.setColor(new Color(0xd8dce6));
            rotated.fillRoundRect(-w / 2, -h / 2, w, h, 8, 8);
            rotated.setColor(new Color(0xff3a44));
            rotated.fillRect(-w / 6, -h / 2 - 5, Math.max(2, w / 3), 7);
        } finally {
            rotated.dispose();
        }
    }

    private static Point pixel(IContainer container, double x, double y) {
        Point p = new Point();
        container.worldToLocal(p, x, y);
        return p;
    }
}
