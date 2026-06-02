package pl.khuzzuk.ui.icons;

import pl.khuzzuk.ui.UiTheme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class DirectoryIcon implements Icon {
    private static final int WIDTH = 30;
    private static final int HEIGHT = 28;
    private final boolean open;

    public DirectoryIcon(boolean open) {
        this.open = open;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(open ? UiTheme.DIRECTORY_TAB_OPEN : UiTheme.DIRECTORY_TAB);
        g.fillRoundRect(x + 3, y + 5, 12, 7, 5, 5);
        g.setColor(open ? UiTheme.DIRECTORY_BODY_OPEN : UiTheme.DIRECTORY_BODY);
        g.fillRoundRect(x + 2, y + 10, 26, 15, 7, 7);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(open ? UiTheme.ACCENT : UiTheme.BORDER);
        g.drawRoundRect(x + 2, y + 10, 25, 14, 7, 7);
        g.setColor(UiTheme.DIRECTORY_HIGHLIGHT);
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
