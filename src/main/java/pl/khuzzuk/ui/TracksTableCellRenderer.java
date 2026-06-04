package pl.khuzzuk.ui;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

class TracksTableCellRenderer extends DefaultTableCellRenderer {
    private static final int CELL_RADIUS = 8;
    private static final int CELL_HORIZONTAL_INSET = 2;
    private static final int CELL_VERTICAL_INSET = 1;
    private static final Color CELL_BORDER = new Color(222, 213, 194);

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean selected,
            boolean focused,
            int row,
            int column) {
        Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        setOpaque(false);
        component.setFont(UiTheme.BODY_FONT);
        if (selected) {
            component.setBackground(UiTheme.SELECTION);
            component.setForeground(UiTheme.INK);
        } else {
            component.setBackground(row % 2 == 0 ? UiTheme.SURFACE : UiTheme.SURFACE_ALT);
            component.setForeground(UiTheme.INK);
        }
        setBorder(UiTheme.empty(0, 10, 0, 10));
        return component;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = CELL_HORIZONTAL_INSET;
            int y = CELL_VERTICAL_INSET;
            int width = getWidth() - CELL_HORIZONTAL_INSET * 2;
            int height = getHeight() - CELL_VERTICAL_INSET * 2;
            graphics2D.setColor(getBackground());
            graphics2D.fillRoundRect(x, y, width, height, CELL_RADIUS, CELL_RADIUS);
            graphics2D.setColor(CELL_BORDER);
            graphics2D.drawRoundRect(x, y, width - 1, height - 1, CELL_RADIUS, CELL_RADIUS);
        } finally {
            graphics2D.dispose();
        }
        super.paintComponent(graphics);
    }
}
