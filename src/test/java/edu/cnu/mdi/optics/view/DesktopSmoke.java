package edu.cnu.mdi.optics.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.DrawnToggleButton;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.optics.OpticsApp;
import edu.cnu.mdi.optics.model.Kind;
import edu.cnu.mdi.optics.model.Piece;
import edu.cnu.mdi.optics.model.Solutions;
import edu.cnu.mdi.util.PropertyUtils;

/**
 * Explicit desktop smoke check; needs a display. Opens the game, places pieces
 * with real mouse events through the palette, and saves screenshots in target/.
 */
public final class DesktopSmoke {

    private static OpticsApp app;
    private static BoardView view;

    public static void main(String[] args) {
        // The window toolkit can keep the process alive, so always exit explicitly.
        try {
            play();
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    private static void play() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            app = new OpticsApp(PropertyUtils.TITLE, "Light Bench smoke test", PropertyUtils.FRACTION, 0.9);
            app.setVisible(true);
            for (JInternalFrame frame : Desktop.getInstance().getAllFrames()) {
                if (frame instanceof BoardView board) {
                    view = board;
                }
            }
        });
        pause();
        try {
            screenshot("level-1-start");

            // Level 1 through the palette and the mouse, as a player would.
            SwingUtilities.invokeAndWait(() -> palette().get(0).doClick());
            click(12, 3);
            check(view.pieces().size() == 1, "click did not place a mirror");
            check(!view.isSolved(), "placing a mirror should not solve level 1 by itself");
            screenshot("level-1-placed");
            for (int i = 0; i < 8 && !view.isSolved(); i++) {
                SwingUtilities.invokeAndWait(() -> view.items().get(0).turn());
            }
            pause();
            screenshot("level-1-after-turning");

            // Drag the piece two columns left, then turn it by its handle.
            drag(pixel(12, 3), pixel(10, 3));
            check(view.items().get(0).getFocus().x == 10, "drag did not move the mirror");
            check(!view.isSolved(), "level 1 still solved after moving the mirror");
            drag(view.items().get(0).getRotatePoint(view.getIContainer()), pixel(10, 6));
            check(view.items().get(0).getAzimuth() == 0, "handle drag did not turn the mirror");
            screenshot("level-1-dragged");

            // A double click turns one step; Delete takes the piece back; both undo.
            doubleClick(10, 3);
            check(view.items().get(0).getAzimuth() == 22.5, "double click did not turn the mirror once, now "
                    + view.items().get(0).getAzimuth());
            key(KeyEvent.VK_DELETE, 0);
            check(view.pieces().isEmpty(), "Delete did not remove the mirror");
            key(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
            check(view.pieces().size() == 1, "undo did not bring the mirror back");

            // A lens level through the mouse: the wrong distance, then drag to the right one.
            SwingUtilities.invokeAndWait(() -> view.load(5));
            pause();
            screenshot("level-6-start");
            SwingUtilities.invokeAndWait(() -> palette().get(Kind.CONVERGING.ordinal()).doClick());
            click(6, 6);
            check(view.pieces().size() == 1 && !view.isSolved(), "lens at the wrong distance");
            screenshot("level-6-unfocused");
            drag(pixel(6, 6), pixel(9, 6));
            check(view.isSolved(), "lens at the focal distance did not solve level 6");
            screenshot("level-6-focused");

            for (int level = 6; level < view.levelCount(); level++) {
                int number = level;
                SwingUtilities.invokeAndWait(() -> solve(number));
                pause();
                screenshot("level-" + (number + 1) + "-solved");
                check(view.isSolved(), "level " + (number + 1) + " not solved");
            }
            System.out.println("Desktop smoke passed; screenshots are in target/.");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                view.prepareForExit();
                for (Window window : Window.getWindows()) {
                    window.dispose();
                }
            });
        }
    }

    private static void doubleClick(int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Component canvas = view.getIContainer().getComponent();
            Point p = pixel(x, y);
            for (int count = 1; count <= 2; count++) {
                for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED,
                        MouseEvent.MOUSE_CLICKED}) {
                    canvas.dispatchEvent(new MouseEvent(canvas, id, System.currentTimeMillis(),
                            id == MouseEvent.MOUSE_PRESSED ? InputEvent.BUTTON1_DOWN_MASK : 0,
                            p.x, p.y, count, false, MouseEvent.BUTTON1));
                }
            }
        });
        pause();
    }

    private static void key(int code, int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(() -> SwingUtilities.processKeyBindings(new KeyEvent(
                view.getIContainer().getComponent(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                modifiers, code, KeyEvent.CHAR_UNDEFINED)));
        pause();
    }

    private static void solve(int level) {
        view.load(level);
        for (Piece wanted : Solutions.ALL.get(level)) {
            OpticItem item = view.place(wanted.kind(), (int) wanted.x(), (int) wanted.y());
            for (int i = 0; i < 8 && Math.abs((item.getAzimuth() - wanted.heading()) % 180) > 1.0e-6; i++) {
                item.turn();
            }
        }
    }

    private static Point pixel(int x, int y) {
        Point p = new Point();
        view.getIContainer().worldToLocal(p, x, y);
        return p;
    }

    private static void drag(Point from, Point to) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Component canvas = view.getIContainer().getComponent();
            mouse(canvas, MouseEvent.MOUSE_PRESSED, from);
            for (int step = 1; step <= 10; step++) {
                mouse(canvas, MouseEvent.MOUSE_DRAGGED, new Point(from.x + (to.x - from.x) * step / 10,
                        from.y + (to.y - from.y) * step / 10));
            }
            mouse(canvas, MouseEvent.MOUSE_RELEASED, to);
        });
        pause();
    }

    private static void mouse(Component canvas, int id, Point p) {
        int down = id == MouseEvent.MOUSE_RELEASED || id == MouseEvent.MOUSE_CLICKED ? 0
                : InputEvent.BUTTON1_DOWN_MASK;
        canvas.dispatchEvent(new MouseEvent(canvas, id, System.currentTimeMillis(), down,
                p.x, p.y, 1, false, MouseEvent.BUTTON1));
    }

    private static void click(int x, int y) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Component canvas = view.getIContainer().getComponent();
            Point p = pixel(x, y);
            for (int id : new int[] {MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED}) {
                mouse(canvas, id, p);
            }
        });
        pause();
    }

    private static List<DrawnToggleButton> palette() {
        List<DrawnToggleButton> found = new ArrayList<>();
        collect(view.getContentPane(), found);
        return found;
    }

    private static void collect(Container root, List<DrawnToggleButton> found) {
        for (Component child : root.getComponents()) {
            if (child instanceof DrawnToggleButton button) {
                found.add(button);
            } else if (child instanceof Container container) {
                collect(container, found);
            }
        }
    }

    private static void screenshot(String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage image = new BufferedImage(app.getWidth(), app.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            app.paint(g);
            g.dispose();
            try {
                ImageIO.write(image, "png", new File("target/" + name + ".png"));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }

    private static void pause() throws InterruptedException {
        Thread.sleep(700);
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }
}
