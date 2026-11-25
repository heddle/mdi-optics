import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.component.DrawnToggleButton;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.optics.OpticsApp;
import edu.cnu.mdi.optics.view.BoardView;

/**
 * Desktop check loaded as a Java agent, so the packaged JAR starts normally. It
 * plays the game with the mouse and reads only what a player can see.
 */
public final class ReleaseDesktopSmoke {
    private static final String STARTUP = "startup";
    // The macOS release job retries this status once.
    private static final int STARTUP_TIMEOUT = 75;
    private static final int LEVELS = 9;
    // Palette buttons are in this order.
    private static final int MIRROR = 0;
    private static final int CONVERGING = 2;

    private static String phase = STARTUP;
    private static OpticsApp app;
    private static BoardView view;

    public static void premain(String args) {
        new Thread(ReleaseDesktopSmoke::verifyDownload, "Packaged app verification").start();
    }

    private static void verifyDownload() {
        try {
            await(() -> {
                app = Arrays.stream(Window.getWindows()).filter(OpticsApp.class::isInstance)
                        .map(OpticsApp.class::cast).findFirst().orElse(null);
                return app != null && app.isShowing();
            });
            await(() -> {
                view = Arrays.stream(Desktop.getInstance().getAllFrames()).filter(BoardView.class::isInstance)
                        .map(BoardView.class::cast).findFirst().orElse(null);
                return view != null && view.isShowing() && view.isMaximum() && says("Level 1 of " + LEVELS);
            });
            // Let the board settle at its maximized size before aiming at grid points.
            Thread.sleep(1000);
            await(ReleaseDesktopSmoke::boardFits);
            System.out.println("Layout passed: the board is maximized with square grid cells.");

            phase = "placing and turning a mirror";
            await(() -> says("0 of 1 targets lit"));
            choose(MIRROR);
            click(12, 3, 1);
            await(() -> button("Undo").isEnabled());
            check(edt(() -> !says("Solved!")), "A new mirror does not solve level 1");
            doubleClick(12, 3);
            doubleClick(12, 3);
            await(() -> says("Solved!"));
            System.out.println("Palette, placing and double-click turning passed.");

            phase = "dragging and undo";
            drag(12, 3, 10, 3);
            await(() -> says("0 of 1 targets lit"));
            press("Undo");
            await(() -> says("Solved!"));
            System.out.println("Dragging and undo passed.");

            phase = "levels";
            for (int level = 2; level <= 6; level++) {
                press("Next");
                int number = level;
                await(() -> says("Level " + number + " of " + LEVELS));
            }

            phase = "lens level";
            choose(CONVERGING);
            click(6, 6, 1);
            await(() -> button("Undo").isEnabled());
            check(edt(() -> says("0 of 1 targets lit")), "A lens at the wrong distance");
            drag(6, 6, 9, 6);
            await(() -> says("Solved!"));
            System.out.println("Lens level passed: focused by dragging the lens.");

            for (int level = 7; level <= LEVELS; level++) {
                press("Next");
                int number = level;
                await(() -> says("Level " + number + " of " + LEVELS));
            }
            check(edt(() -> !button("Next").isEnabled()), "Level " + LEVELS + " is the last");
            System.out.println("All " + LEVELS + " levels loaded from the packaged app.");

            phase = "application shutdown";
            edt(() -> {
                app.dispatchEvent(new WindowEvent(app, WindowEvent.WINDOW_CLOSING));
                return null;
            });
        } catch (Throwable e) {
            System.err.println("Desktop smoke test failed during " + phase);
            e.printStackTrace();
            Runtime.getRuntime().halt(STARTUP.equals(phase) && e instanceof TimeoutException ? STARTUP_TIMEOUT : 1);
        }
    }

    private static Component canvas() {
        return view.getIContainer().getComponent();
    }

    private static Point pixel(int x, int y) {
        Point point = new Point();
        view.getIContainer().worldToLocal(point, x, y);
        return point;
    }

    private static boolean boardFits() {
        Component canvas = canvas();
        int across = pixel(1, 0).x - pixel(0, 0).x;
        int up = pixel(0, 0).y - pixel(0, 1).y;
        return across > 10 && Math.abs(across - up) <= 1
                && canvas.contains(pixel(0, 0)) && canvas.contains(pixel(16, 12));
    }

    private static <T extends Component> List<T> find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            } else if (child instanceof Container group) {
                found.addAll(find(group, type));
            }
        }
        return found;
    }

    private static boolean says(String text) {
        return find(view.getContentPane(), JLabel.class).stream()
                .anyMatch(label -> label.getText() != null && label.getText().contains(text));
    }

    private static JButton button(String text) {
        return find(view.getContentPane(), JButton.class).stream()
                .filter(candidate -> text.equals(candidate.getText())).findFirst().orElseThrow();
    }

    private static void press(String text) throws Exception {
        edt(() -> {
            check(button(text).isShowing() && button(text).isEnabled(), text + " enabled and visible");
            button(text).doClick(0);
            return null;
        });
    }

    private static void choose(int index) throws Exception {
        edt(() -> {
            DrawnToggleButton piece = find(view.getContentPane(), DrawnToggleButton.class).get(index);
            check(piece.isShowing() && piece.isEnabled(), "Palette piece " + index + " available");
            piece.doClick(0);
            return null;
        });
    }

    private static void mouse(int id, Point at, int clicks) {
        int down = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED
                ? InputEvent.BUTTON1_DOWN_MASK : 0;
        canvas().dispatchEvent(new MouseEvent(canvas(), id, System.currentTimeMillis(), down,
                at.x, at.y, clicks, false, MouseEvent.BUTTON1));
    }

    private static void click(int x, int y, int clicks) throws Exception {
        edt(() -> {
            Point at = pixel(x, y);
            mouse(MouseEvent.MOUSE_PRESSED, at, clicks);
            mouse(MouseEvent.MOUSE_RELEASED, at, clicks);
            mouse(MouseEvent.MOUSE_CLICKED, at, clicks);
            return null;
        });
        Thread.sleep(300);
    }

    private static void doubleClick(int x, int y) throws Exception {
        click(x, y, 1);
        click(x, y, 2);
    }

    private static void drag(int fromX, int fromY, int toX, int toY) throws Exception {
        edt(() -> {
            Point from = pixel(fromX, fromY);
            Point to = pixel(toX, toY);
            mouse(MouseEvent.MOUSE_PRESSED, from, 1);
            for (int step = 1; step <= 10; step++) {
                mouse(MouseEvent.MOUSE_DRAGGED, new Point(from.x + (to.x - from.x) * step / 10,
                        from.y + (to.y - from.y) * step / 10), 1);
            }
            mouse(MouseEvent.MOUSE_RELEASED, to, 1);
            return null;
        });
        Thread.sleep(300);
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        do {
            if (edt(condition)) {
                return;
            }
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        throw new TimeoutException("Timed out waiting for desktop state");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
