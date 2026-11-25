package edu.cnu.mdi.optics;

import java.beans.PropertyVetoException;

import javax.swing.SwingUtilities;

import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.optics.view.BoardView;
import edu.cnu.mdi.util.PropertyUtils;

/** Light Bench: steer a laser onto its targets with mirrors and beam splitters. */
public final class OpticsApp extends BaseMDIApplication {

    public OpticsApp(Object... keyVals) {
        super(keyVals);
    }

    @Override
    protected void addInitialViews() {
        BoardView board = new BoardView();
        // The game has one view, so let it fill the window once both are laid out.
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            try {
                board.setMaximum(true);
            } catch (PropertyVetoException e) {
                // It stays at its own size, which still works.
            }
        }));
    }

    @Override
    protected boolean exitOnClose() {
        return true;
    }

    public static void main(String[] args) {
        BaseMDIApplication.launch(() -> new OpticsApp(
                PropertyUtils.TITLE, "MDI Light Bench",
                PropertyUtils.FRACTION, 0.9));
    }
}
