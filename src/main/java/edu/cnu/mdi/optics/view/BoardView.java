package edu.cnu.mdi.optics.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.DrawnToggleButton;
import edu.cnu.mdi.container.BaseContainer;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.toolbar.ToolBits;
import edu.cnu.mdi.item.AItem;
import edu.cnu.mdi.optics.model.Kind;
import edu.cnu.mdi.optics.model.Level;
import edu.cnu.mdi.optics.model.Levels;
import edu.cnu.mdi.optics.model.Piece;
import edu.cnu.mdi.optics.model.RayTracer;
import edu.cnu.mdi.undo.ItemEdits;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * The game: a board of grid points with a palette beside it.
 *
 * <p>The board's items are the only record of what the player has done. The
 * beam, the lit targets and the number of pieces left are all worked out from
 * them, so undo, redo and delete need no bookkeeping of their own.</p>
 */
public final class BoardView extends BaseView {

    private static final Rectangle2D.Double WORLD = new Rectangle2D.Double(-1, -1,
            Level.WIDTH + 2, Level.HEIGHT + 2);
    private static final int PANEL_WIDTH = 190;
    /** Upright, so that no level is solved just by putting a piece down. */
    private static final double NEW_PIECE_HEADING = 90;

    private final List<Level> levels = Levels.load();
    private int index;

    private final Map<Kind, DrawnToggleButton> palette = new EnumMap<>(Kind.class);
    private final JLabel title = new JLabel();
    private final JLabel hint = new JLabel();
    private final JLabel status = new JLabel();
    private final JButton undo = button("Undo");
    private final JButton redo = button("Redo");
    private final JButton previous = button("Previous");
    private final JButton next = button("Next");

    public BoardView() {
        super(PropertyUtils.TITLE, "Light Bench",
                PropertyUtils.WIDTH, 900,
                PropertyUtils.HEIGHT, 700,
                PropertyUtils.WORLDSYSTEM, WORLD,
                PropertyUtils.TOOLBARBITS, ToolBits.POINTER | ToolBits.DELETE,
                PropertyUtils.VISIBLE, true);

        getContentPane().add(buildPanel(), BorderLayout.WEST);
        pack();

        getIContainer().setBeforeDraw(new DrawableAdapter() {
            @Override
            public void draw(Graphics2D g, IContainer container) {
                // Traced on every repaint, so the beam follows a piece while it is dragged.
                Scenery.draw(g, container, level(), trace());
            }
        });
        getDefaultLayer().addItemChangeListener((layer, item, type) -> update());
        canvas().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                keepSquare();
            }
        });
        canvas().addMouseListener(new MouseAdapter() {
            private Point pressed;

            @Override
            public void mousePressed(MouseEvent event) {
                pressed = event.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Point released = event.getPoint();
                boolean click = SwingUtilities.isLeftMouseButton(event) && pressed != null
                        && pressed.distance(released) < 6;
                if (click && armed() != null) {
                    // After the pointer tool has finished with this click.
                    SwingUtilities.invokeLater(() -> placeAt(released));
                }
            }
        });
        load(0);
    }

    /** The level being played. */
    Level level() {
        return levels.get(index);
    }

    int levelCount() {
        return levels.size();
    }

    BaseContainer board() {
        return (BaseContainer) getIContainer();
    }

    private java.awt.Component canvas() {
        return getIContainer().getComponent();
    }

    /** Clears the board and starts level {@code number}, counting from zero. */
    void load(int number) {
        index = Math.max(0, Math.min(levels.size() - 1, number));
        for (AItem item : getDefaultLayer().getAllItems()) {
            getDefaultLayer().deleteItem(item);
        }
        board().clearUndoHistory();
        palette.values().forEach(button -> button.setSelected(false));
        title.setText(html("<b>Level " + (index + 1) + " of " + levels.size() + ": " + level().name() + "</b>"));
        hint.setText(html(level().hint()));
        update();
    }

    /** The pieces on the board, as the ray tracer sees them. */
    List<Piece> pieces() {
        List<Piece> pieces = new ArrayList<>();
        for (OpticItem item : items()) {
            pieces.add(item.piece());
        }
        return pieces;
    }

    List<OpticItem> items() {
        List<OpticItem> items = new ArrayList<>();
        for (AItem item : getDefaultLayer().getAllItems()) {
            // An item announces itself before its constructor has given it a position.
            if (item instanceof OpticItem optic && optic.getFocus() != null) {
                items.add(optic);
            }
        }
        return items;
    }

    private RayTracer.Result trace() {
        return RayTracer.trace(level(), pieces());
    }

    boolean isSolved() {
        return level().isSolvedBy(trace());
    }

    int remaining(Kind kind) {
        return level().allowed(kind) - (int) items().stream().filter(item -> item.kind() == kind).count();
    }

    private boolean isOpen(Point point, OpticItem mover) {
        if (!level().isOpen(point.x, point.y)) {
            return false;
        }
        for (OpticItem item : items()) {
            if (item != mover && item.getFocus().x == point.x && item.getFocus().y == point.y) {
                return false;
            }
        }
        return true;
    }

    private Kind armed() {
        for (Map.Entry<Kind, DrawnToggleButton> entry : palette.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void placeAt(Point screen) {
        Kind kind = armed();
        if (kind == null || getIContainer().getItemAtPoint(screen) != null) {
            return;
        }
        Point2D.Double world = new Point2D.Double();
        getIContainer().localToWorld(screen, world);
        place(kind, (int) Math.round(world.x), (int) Math.round(world.y));
    }

    /**
     * Puts a new piece on a grid point as one undoable edit.
     *
     * @return the piece, or {@code null} if the point is taken or none are left
     */
    OpticItem place(Kind kind, int x, int y) {
        if (remaining(kind) <= 0 || !isOpen(new Point(x, y), null)) {
            return null;
        }
        OpticItem item = new OpticItem(getDefaultLayer(), kind, x, y, NEW_PIECE_HEADING,
                level().focalLength(), this::isOpen);
        board().recordEdit(ItemEdits.created("Place " + item.getDisplayName(), board(), item));
        board().selectAllItems(false);
        getDefaultLayer().selectItem(item, true);
        palette.get(kind).setSelected(false);
        board().setToolBarState();
        update();
        return item;
    }

    /** Takes every piece off the board; this can be undone. */
    void clearBoard() {
        board().selectAllItems(true);
        board().deleteSelectedItemsUndoably();
        update();
    }

    /** Brings the palette, the buttons and the status line up to date with the board. */
    private void update() {
        palette.forEach((kind, button) -> {
            int left = remaining(kind);
            button.setVisible(level().allowed(kind) > 0);
            button.setEnabled(left > 0);
            if (left <= 0) {
                button.setSelected(false);
            }
            button.repaint();
        });
        RayTracer.Result beam = trace();
        int lit = 0;
        for (int i = 0; i < level().targets().size(); i++) {
            lit += level().isLit(i, beam) ? 1 : 0;
        }
        boolean solved = level().isSolvedBy(beam);
        boolean last = index == levels.size() - 1;
        status.setText(html(solved
                ? "<b><font color='#157f3b'>Solved!" + (last ? " That was the last level." : "") + "</font></b>"
                : lit + " of " + level().targets().size() + " targets lit"));
        undo.setEnabled(board().canUndo());
        redo.setEnabled(board().canRedo());
        previous.setEnabled(index > 0);
        next.setEnabled(!last);
        board().refresh();
        // Edits are recorded after listeners hear about a gesture, so look again once it is over.
        SwingUtilities.invokeLater(() -> {
            undo.setEnabled(board().canUndo());
            redo.setEnabled(board().canRedo());
        });
    }

    private JComponent buildPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(panel, title);
        panel.add(Box.createVerticalStrut(8));
        add(panel, hint);
        panel.add(Box.createVerticalStrut(14));

        for (Kind kind : Kind.values()) {
            DrawnToggleButton button = new DrawnToggleButton(
                    new PalettePainter(kind, () -> remaining(kind), () -> level().focalLength()));
            button.setToolTipText("Click here, then click a grid point");
            button.setRolloverEnabled(true);
            button.setMaximumSize(button.getPreferredSize());
            button.addActionListener(e -> palette.forEach((other, otherButton) -> {
                if (other != kind) {
                    otherButton.setSelected(false);
                }
            }));
            palette.put(kind, button);
            add(panel, button);
            panel.add(Box.createVerticalStrut(6));
        }

        panel.add(Box.createVerticalStrut(10));
        add(panel, status);
        panel.add(Box.createVerticalGlue());

        JPanel buttons = new JPanel(new GridLayout(0, 2, 6, 6));
        JButton clear = button("Clear");
        undo.addActionListener(e -> {
            board().undo();
            update();
        });
        redo.addActionListener(e -> {
            board().redo();
            update();
        });
        clear.addActionListener(e -> clearBoard());
        previous.addActionListener(e -> load(index - 1));
        next.addActionListener(e -> load(index + 1));
        buttons.add(undo);
        buttons.add(redo);
        buttons.add(clear);
        buttons.add(new JLabel());
        buttons.add(previous);
        buttons.add(next);
        buttons.setMaximumSize(new Dimension(PANEL_WIDTH, buttons.getPreferredSize().height));
        add(panel, buttons);

        panel.setPreferredSize(new Dimension(PANEL_WIDTH + 20, 500));
        return panel;
    }

    private static void add(JPanel panel, JComponent child) {
        child.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(child);
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        return button;
    }

    private static String html(String text) {
        return "<html><div style='width:" + (PANEL_WIDTH - 40) + "px'>" + text + "</div></html>";
    }

    /** Widens or heightens the visible world so grid cells stay square. */
    private void keepSquare() {
        Dimension size = canvas().getSize();
        if (size.width <= 0 || size.height <= 0) {
            return;
        }
        double pixels = Math.min(size.width / WORLD.width, size.height / WORLD.height);
        double width = size.width / pixels;
        double height = size.height / pixels;
        getIContainer().setWorldSystem(new Rectangle2D.Double(WORLD.getCenterX() - width / 2,
                WORLD.getCenterY() - height / 2, width, height));
        board().refresh();
    }
}
