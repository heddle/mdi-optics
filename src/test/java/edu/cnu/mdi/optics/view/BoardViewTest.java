package edu.cnu.mdi.optics.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.cnu.mdi.container.BaseToolHandler;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.graphics.toolbar.GestureContext;
import edu.cnu.mdi.optics.model.Kind;
import edu.cnu.mdi.optics.model.Piece;
import edu.cnu.mdi.optics.model.Solutions;

class BoardViewTest {

    private BoardView view;

    @BeforeEach
    void openView() throws Exception {
        onEdt(() -> {
            view = new BoardView();
            view.board().setBounds(0, 0, 900, 700);
            view.board().setDirty(true);
        });
    }

    @Test
    void placingAPieceUsesItUpAndUndoGivesItBack() throws Exception {
        onEdt(() -> {
            assertEquals(1, view.remaining(Kind.MIRROR));

            assertNotNull(view.place(Kind.MIRROR, 5, 5));
            assertEquals(0, view.remaining(Kind.MIRROR));
            assertNull(view.place(Kind.MIRROR, 6, 6), "none left");

            assertTrue(view.board().undo());
            assertEquals(1, view.remaining(Kind.MIRROR));
            assertTrue(view.pieces().isEmpty());

            assertTrue(view.board().redo());
            assertEquals(0, view.remaining(Kind.MIRROR));
            assertEquals(1, view.pieces().size());
        });
    }

    @Test
    void deletingAPieceCanBeUndone() throws Exception {
        onEdt(() -> {
            view.place(Kind.MIRROR, 5, 5);

            view.board().deleteSelectedItemsUndoably();
            assertEquals(1, view.remaining(Kind.MIRROR));

            view.board().undo();
            assertEquals(0, view.remaining(Kind.MIRROR));
        });
    }

    @Test
    void piecesCannotGoOnSceneryOrOnEachOther() throws Exception {
        onEdt(() -> {
            assertNull(view.place(Kind.MIRROR, 1, 3), "laser");
            assertNull(view.place(Kind.MIRROR, 12, 9), "target");
            assertNull(view.place(Kind.MIRROR, 40, 3), "off the board");
            view.load(1);
            assertNotNull(view.place(Kind.MIRROR, 5, 5));
            assertNull(view.place(Kind.MIRROR, 5, 5), "occupied");
        });
    }

    @Test
    void aDraggedPieceSnapsToTheGridAndUndoes() throws Exception {
        onEdt(() -> {
            OpticItem item = view.place(Kind.MIRROR, 5, 5);

            drag(item, pixel(5, 5), pixel(8.3, 2.8));
            assertPosition(item, 8, 3);

            view.board().undo();
            assertPosition(item, 5, 5);
        });
    }

    @Test
    void aPieceStopsShortOfScenery() throws Exception {
        onEdt(() -> {
            OpticItem item = view.place(Kind.MIRROR, 5, 5);

            drag(item, pixel(5, 5), pixel(12, 9));

            assertPosition(item, 5, 5);
        });
    }

    @Test
    void draggingTheHandleTurnsInSteps() throws Exception {
        onEdt(() -> {
            OpticItem item = view.place(Kind.MIRROR, 5, 5);

            // Toward a point a little off due north of the piece.
            drag(item, item.getRotatePoint(view.board()), pixel(5.2, 8));
            assertEquals(0, item.getAzimuth(), 1.0e-9);
            assertPosition(item, 5, 5);

            view.board().undo();
            assertEquals(90, item.getAzimuth(), 1.0e-9);
        });
    }

    @Test
    void doubleClickingTurnsOneStep() throws Exception {
        onEdt(() -> {
            OpticItem item = view.place(Kind.MIRROR, 5, 5);

            item.turn();
            assertEquals(112.5, item.getAzimuth(), 1.0e-9);

            view.board().undo();
            assertEquals(90, item.getAzimuth(), 1.0e-9);
        });
    }

    @Test
    void clearingTheBoardCanBeUndone() throws Exception {
        onEdt(() -> {
            view.load(1);
            view.place(Kind.MIRROR, 5, 5);
            view.place(Kind.MIRROR, 6, 6);

            view.clearBoard();
            assertTrue(view.pieces().isEmpty());

            view.board().undo();
            assertEquals(2, view.pieces().size());
        });
    }

    @Test
    void anotherLevelStartsWithAnEmptyBoardAndNoHistory() throws Exception {
        onEdt(() -> {
            view.place(Kind.MIRROR, 5, 5);

            view.load(1);

            assertTrue(view.pieces().isEmpty());
            assertFalse(view.board().canUndo());
            assertEquals(2, view.remaining(Kind.MIRROR));
        });
    }

    @Test
    void puttingASolutionPieceDownDoesNotSolveALevelUntilItIsTurned() throws Exception {
        onEdt(() -> {
            Piece wanted = Solutions.ALL.get(0).get(0);

            view.place(wanted.kind(), (int) wanted.x(), (int) wanted.y());

            assertFalse(view.isSolved());
        });
    }

    @Test
    void everyLevelCanBeSolvedOnTheBoard() throws Exception {
        onEdt(() -> {
            for (int level = 0; level < view.levelCount(); level++) {
                view.load(level);
                for (Piece wanted : Solutions.ALL.get(level)) {
                    assertFalse(view.isSolved(), "solved early: level " + (level + 1));
                    OpticItem item = view.place(wanted.kind(), (int) wanted.x(), (int) wanted.y());
                    assertNotNull(item, "level " + (level + 1) + " " + wanted);
                    // Both faces work, so headings 180 degrees apart are the same.
                    for (int i = 0; i < 8 && Math.abs((item.getAzimuth() - wanted.heading()) % 180) > 1.0e-6; i++) {
                        item.turn();
                    }
                }
                assertTrue(view.isSolved(), "level " + (level + 1));
            }
        });
    }

    // ----------------------------------------------------------------- helpers

    private Point pixel(double x, double y) {
        Point point = new Point();
        view.board().worldToLocal(point, x, y);
        return point;
    }

    private static void assertPosition(OpticItem item, double x, double y) {
        assertEquals(x, item.getFocus().x, 1.0e-9, "x");
        assertEquals(y, item.getFocus().y, 1.0e-9, "y");
    }

    private void drag(OpticItem item, Point from, Point to) {
        BaseToolHandler handler = new BaseToolHandler(view.board());
        BaseToolBar bar = new BaseToolBar(view.board().getComponent(), handler, 0L);
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        handler.beginDragObject(context(bar, item, from, MouseEvent.MOUSE_PRESSED));
        handler.dragObjectBy(context(bar, item, to, MouseEvent.MOUSE_DRAGGED), dx, dy);
        handler.endDragObject(context(bar, item, to, MouseEvent.MOUSE_RELEASED));
    }

    private GestureContext context(BaseToolBar bar, OpticItem item, Point at, int id) {
        MouseEvent event = new MouseEvent(view.board().getComponent(), id, 0, MouseEvent.BUTTON1_DOWN_MASK,
                at.x, at.y, 1, false, MouseEvent.BUTTON1);
        return new GestureContext(bar, view.board().getComponent(), item, at, event);
    }

    private static void onEdt(Runnable action) throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(action);
    }
}
