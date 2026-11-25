package edu.cnu.mdi.optics.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.item.ItemChangeType;
import edu.cnu.mdi.item.ItemModification;
import edu.cnu.mdi.item.ItemState;
import edu.cnu.mdi.item.Layer;
import edu.cnu.mdi.item.PathBasedItem;
import edu.cnu.mdi.optics.model.Geometry;
import edu.cnu.mdi.optics.model.Kind;
import edu.cnu.mdi.optics.model.Piece;
import edu.cnu.mdi.undo.ItemEdits;

/**
 * A mirror, splitter or lens the player has placed. It moves from grid point to grid
 * point and turns in steps of {@value #TURN} degrees; its azimuth is the
 * direction its face points.
 *
 * <p>Position, azimuth and path are all the state it has, so the undo support
 * inherited from {@link PathBasedItem} is complete.</p>
 */
final class OpticItem extends PathBasedItem {

    /** The step, in degrees, that a piece turns in. */
    static final double TURN = 22.5;

    private static final double HALF_THICKNESS = 0.09;
    private static final int HANDLE_PIXELS = 34;
    private static final int GRAB_PIXELS = 9;

    private static final Color HANDLE = new Color(0xffd24d);

    private final Kind kind;
    private final double focalLength;
    private final BiPredicate<Point, OpticItem> open;

    /**
     * @param focalLength the level's focal length, used only by lenses
     * @param open        whether a grid point is free for the given piece to move to
     */
    OpticItem(Layer layer, Kind kind, int x, int y, double heading, double focalLength,
            BiPredicate<Point, OpticItem> open) {
        super(layer);
        this.kind = kind;
        this.focalLength = focalLength;
        this.open = open;
        setDisplayName(name(kind));
        setLocked(false);
        setSelectable(true);
        setDraggable(true);
        setRotatable(true);
        setResizable(false);
        setDeletable(true);
        setConnectable(false);
        setRightClickable(false);
        setDoubleClickable(true);
        setStyleEditable(false);
        setFillFocus(false);
        place(x, y, heading);
    }

    /** What a kind of piece is called. */
    static String name(Kind kind) {
        return switch (kind) {
            case MIRROR -> "Mirror";
            case SPLITTER -> "Splitter";
            case CONVERGING -> "Converging lens";
            case DIVERGING -> "Diverging lens";
        };
    }

    /** This item as the ray tracer sees it. */
    Piece piece() {
        Point2D.Double center = getFocus();
        return new Piece(kind, center.x, center.y, getAzimuth());
    }

    Kind kind() {
        return kind;
    }

    private void place(double x, double y, double heading) {
        double tx = Geometry.dirY(heading) * Piece.LENGTH / 2;
        double ty = -Geometry.dirX(heading) * Piece.LENGTH / 2;
        double nx = Geometry.dirX(heading) * HALF_THICKNESS;
        double ny = Geometry.dirY(heading) * HALF_THICKNESS;
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x - tx - nx, y - ty - ny);
        path.lineTo(x + tx - nx, y + ty - ny);
        path.lineTo(x + tx + nx, y + ty + ny);
        path.lineTo(x - tx + nx, y - ty + ny);
        path.closePath();
        _path = path;
        _focus = new Point2D.Double(x, y);
        setAzimuth(heading);
        setDirty(true);
    }

    @Override
    public void modify() {
        IContainer container = _modification.getContainer();
        Point2D.Double start = _modification.getStartFocus();
        Point2D.Double mouse = _modification.getCurrentWorldPoint();
        if (_modification.getType() == ItemModification.ModificationType.ROTATE) {
            double dx = mouse.x - start.x;
            double dy = mouse.y - start.y;
            if (dx != 0 || dy != 0) {
                place(start.x, start.y, snap(Math.toDegrees(Math.atan2(dx, dy))));
            }
        } else if (_modification.getType() == ItemModification.ModificationType.DRAG) {
            Point2D.Double from = _modification.getStartWorldPoint();
            int x = (int) Math.round(start.x + mouse.x - from.x);
            int y = (int) Math.round(start.y + mouse.y - from.y);
            if (open.test(new Point(x, y), this)) {
                place(x, y, getAzimuth());
            }
        }
        container.refresh();
    }

    private static double snap(double heading) {
        return Math.round(heading / TURN) * TURN;
    }

    /** Turns one step clockwise, as one undoable edit. */
    void turn() {
        ItemState before = captureState();
        Point2D.Double center = getFocus();
        place(center.x, center.y, getAzimuth() + TURN);
        if (getContainer() instanceof BaseContainer container) {
            container.recordEdit(ItemEdits.changedStates("Rotate", container, List.of(this),
                    Collections.singletonList(before), ItemChangeType.ROTATED));
            getLayer().notifyItemChangeListeners(this, ItemChangeType.ROTATED);
            container.refresh();
        }
    }

    @Override
    public void doubleClicked(MouseEvent event) {
        turn();
    }

    private Point2D[] ends(IContainer container) {
        Point2D.Double center = getFocus();
        double tx = Geometry.dirY(getAzimuth()) * Piece.LENGTH / 2;
        double ty = -Geometry.dirX(getAzimuth()) * Piece.LENGTH / 2;
        Point a = new Point();
        Point b = new Point();
        container.worldToLocal(a, center.x - tx, center.y - ty);
        container.worldToLocal(b, center.x + tx, center.y + ty);
        return new Point2D[] {a, b};
    }

    @Override
    public void drawItem(Graphics2D original, IContainer container) {
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Point2D[] ends = ends(container);
            if (isSelected()) {
                g.setColor(new Color(255, 255, 255, 70));
                g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(ends[0], ends[1]));
            }
            PieceArt.draw(g, kind, ends[0], ends[1], 5f);
        } finally {
            g.dispose();
        }
    }

    /** Draws the turn handle brightly; MDI's standard one is hard to see on a dark board. */
    @Override
    public void drawSelections(Graphics2D original, IContainer container) {
        if (!isSelected()) {
            return;
        }
        Graphics2D g = (Graphics2D) original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Point center = getFocusPoint(container);
            Point handle = getRotatePoint(container);
            if (kind.isLens()) {
                drawFocalPoints(g, container);
            }
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(HANDLE);
            g.draw(new Line2D.Double(center, handle));
            g.fillOval(handle.x - RPSIZE2, handle.y - RPSIZE2, RPSIZE, RPSIZE);
            g.setColor(Color.WHITE);
            g.drawOval(handle.x - RPSIZE2, handle.y - RPSIZE2, RPSIZE, RPSIZE);
        } finally {
            g.dispose();
        }
    }

    /** Marks the points one focal length away on each side of a lens. */
    private void drawFocalPoints(Graphics2D g, IContainer container) {
        Point2D.Double lens = getFocus();
        double dx = Geometry.dirX(getAzimuth()) * focalLength;
        double dy = Geometry.dirY(getAzimuth()) * focalLength;
        Point near = new Point();
        Point far = new Point();
        container.worldToLocal(near, lens.x - dx, lens.y - dy);
        container.worldToLocal(far, lens.x + dx, lens.y + dy);

        g.setColor(new Color(HANDLE.getRed(), HANDLE.getGreen(), HANDLE.getBlue(), 110));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[] {3f, 6f}, 0f));
        g.draw(new Line2D.Double(near, far));
        g.setStroke(new BasicStroke(2f));
        g.setColor(HANDLE);
        for (Point p : new Point[] {near, far}) {
            g.drawLine(p.x - 5, p.y - 5, p.x + 5, p.y + 5);
            g.drawLine(p.x - 5, p.y + 5, p.x + 5, p.y - 5);
        }
    }

    @Override
    public boolean contains(IContainer container, Point point) {
        Point2D[] ends = ends(container);
        return new Line2D.Double(ends[0], ends[1]).ptSegDist(point) <= GRAB_PIXELS
                || inRotatePoint(container, point);
    }

    @Override
    public Point getRotatePoint(IContainer container) {
        Point center = getFocusPoint(container);
        // Screen y runs downward.
        return new Point(center.x + (int) Math.round(HANDLE_PIXELS * Geometry.dirX(getAzimuth())),
                center.y - (int) Math.round(HANDLE_PIXELS * Geometry.dirY(getAzimuth())));
    }

    /** Pieces cannot be resized, so they have no resize handles. */
    @Override
    public Point[] getSelectionPoints(IContainer container) {
        return new Point[0];
    }

    @Override
    public void getFeedbackStrings(IContainer container, Point screenPoint, Point2D.Double worldPoint,
            List<String> feedback) {
        if (contains(container, screenPoint)) {
            feedback.add(getDisplayName() + " facing " + ((getAzimuth() + 360) % 360) + "°"
                    + (kind.isLens() ? ", focal length " + focalLength : ""));
        }
    }
}
