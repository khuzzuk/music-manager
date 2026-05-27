package pl.khuzzuk.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

class TreeToggleIcon implements Icon {
    private static final int SIZE = 16;
    private final boolean expanded;

    TreeToggleIcon(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(96, 116, 140));

        int centerY = y + SIZE / 2;
        if (expanded) {
            g.drawLine(x + 4, centerY - 2, x + SIZE / 2, centerY + 2);
            g.drawLine(x + SIZE / 2, centerY + 2, x + SIZE - 4, centerY - 2);
        } else {
            g.drawLine(x + 6, y + 4, x + 10, centerY);
            g.drawLine(x + 10, centerY, x + 6, y + SIZE - 4);
        }

        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
