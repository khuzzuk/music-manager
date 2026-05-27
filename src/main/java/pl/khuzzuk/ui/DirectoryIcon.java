package pl.khuzzuk.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;

class DirectoryIcon implements Icon {
    private static final int WIDTH = 30;
    private static final int HEIGHT = 28;
    private final boolean open;

    DirectoryIcon(boolean open) {
        this.open = open;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color tabColor = open ? new Color(157, 196, 232) : new Color(171, 190, 214);
        Color bodyColor = open ? new Color(215, 233, 248) : new Color(225, 232, 241);
        Color borderColor = open ? new Color(82, 132, 184) : new Color(106, 129, 157);

        g.setColor(tabColor);
        g.fillRoundRect(x + 3, y + 5, 12, 7, 5, 5);
        g.setColor(bodyColor);
        g.fillRoundRect(x + 2, y + 10, 26, 15, 7, 7);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(borderColor);
        g.drawRoundRect(x + 2, y + 10, 25, 14, 7, 7);
        g.setColor(new Color(255, 255, 255, 135));
        g.drawLine(x + 6, y + 13, x + 24, y + 13);

        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }
}
