package edu.cnu.mdi.optics.view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.function.IntSupplier;

import javax.swing.AbstractButton;

import edu.cnu.mdi.component.ButtonPainter;
import edu.cnu.mdi.optics.model.Kind;

/** A palette button: the piece itself, its name and how many are left. */
final class PalettePainter implements ButtonPainter {

    private static final Dimension SIZE = new Dimension(170, 64);

    private final Kind kind;
    private final IntSupplier remaining;

    PalettePainter(Kind kind, IntSupplier remaining) {
        this.kind = kind;
        this.remaining = remaining;
    }

    @Override
    public Dimension getPreferredSize() {
        return SIZE;
    }

    @Override
    public void draw(Graphics2D g, AbstractButton button, Rectangle bounds, long frameCount) {
        boolean enabled = button.isEnabled();
        boolean selected = button.getModel().isSelected();
        boolean rollover = enabled && button.getModel().isRollover();

        g.setColor(new Color(rollover ? 0x2c3852 : 0x222c42));
        g.fillRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 14, 14);
        g.setColor(selected ? new Color(0xffd24d) : new Color(0x47546f));
        g.setStroke(new BasicStroke(selected ? 3f : 1f));
        g.drawRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 14, 14);

        if (!enabled) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        }
        int middle = bounds.y + bounds.height / 2;
        PieceArt.draw(g, kind, new Point2D.Double(bounds.x + 18, middle + 15),
                new Point2D.Double(bounds.x + 48, middle - 15), 5f);

        g.setColor(Color.WHITE);
        g.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(kind == Kind.MIRROR ? "Mirror" : "Splitter", bounds.x + 66, middle - 2);
        g.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        g.drawString(remaining.getAsInt() + " left", bounds.x + 66, middle + 15);
    }
}
