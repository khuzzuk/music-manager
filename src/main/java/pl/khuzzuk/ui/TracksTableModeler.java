package pl.khuzzuk.ui;

import javax.swing.JTable;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.Icon;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;

class TracksTableModeler {
    private static final Color HEADER_BACKGROUND = UiTheme.DARK_PANEL_ALT;
    private static final Color HEADER_FOREGROUND = UiTheme.SELECTION_STRONG;
    private static final Color HEADER_BORDER = UiTheme.DARK_PANEL_LINE;

    void modelTable(JTable table) {
        UiTheme.modelTable(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Object.class, new TrackCellRenderer());
        modelHeader(table.getTableHeader());
    }

    void modelColumnMenu(JPopupMenu menu) {
        menu.setBackground(UiTheme.SURFACE);
        menu.setBorder(UiTheme.lineBorder());
    }

    void modelColumnMenuItem(JCheckBoxMenuItem item) {
        item.setFont(UiTheme.BODY_FONT);
        item.setForeground(UiTheme.INK);
        item.setBackground(UiTheme.SURFACE);
        item.setBorder(UiTheme.empty(5, 10, 5, 10));
    }

    private void modelHeader(JTableHeader header) {
        header.setDefaultRenderer(new TrackHeaderRenderer(header.getDefaultRenderer()));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);
        header.setBackground(HEADER_BACKGROUND);
        header.setForeground(HEADER_FOREGROUND);
        header.setBorder(UiTheme.lineBorder());
    }

    private static class TrackHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        private TrackHeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            Component component = delegate.getTableCellRendererComponent(
                    table,
                    value == null ? "" : value.toString().toUpperCase(),
                    selected,
                    focused,
                    row,
                    column);
            component.setFont(UiTheme.BODY_BOLD_FONT);
            component.setBackground(HEADER_BACKGROUND);
            component.setForeground(HEADER_FOREGROUND);

            if (component instanceof JLabel label) {
                label.setOpaque(true);
                label.setHorizontalAlignment(JLabel.LEFT);
                label.setHorizontalTextPosition(JLabel.LEFT);
                label.setIcon(sortIcon(table, column));
                label.setIconTextGap(7);
                label.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                        javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 1, HEADER_BORDER),
                        UiTheme.empty(0, 10, 0, 8)));
            }
            return component;
        }

        private Icon sortIcon(JTable table, int column) {
            if (table == null || table.getRowSorter() == null || column < 0) {
                return null;
            }

            int modelColumn = table.convertColumnIndexToModel(column);
            List<? extends RowSorter.SortKey> sortKeys = table.getRowSorter().getSortKeys();
            for (RowSorter.SortKey sortKey : sortKeys) {
                if (sortKey.getColumn() == modelColumn && sortKey.getSortOrder() != SortOrder.UNSORTED) {
                    return new SortArrowIcon(sortKey.getSortOrder());
                }
            }
            return null;
        }
    }

    private static class SortArrowIcon implements Icon {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 7;

        private final SortOrder sortOrder;

        private SortArrowIcon(SortOrder sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(UiTheme.ACCENT);
                graphics2D.fillPolygon(createArrow(x, y));
            } finally {
                graphics2D.dispose();
            }
        }

        private Polygon createArrow(int x, int y) {
            Polygon arrow = new Polygon();
            if (sortOrder == SortOrder.DESCENDING) {
                arrow.addPoint(x, y);
                arrow.addPoint(x + WIDTH, y);
                arrow.addPoint(x + WIDTH / 2, y + HEIGHT);
            } else {
                arrow.addPoint(x, y + HEIGHT);
                arrow.addPoint(x + WIDTH, y + HEIGHT);
                arrow.addPoint(x + WIDTH / 2, y);
            }
            return arrow;
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

    private static class TrackCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            component.setFont(UiTheme.BODY_FONT);
            if (selected) {
                component.setBackground(UiTheme.SELECTION);
                component.setForeground(UiTheme.INK);
            } else {
                component.setBackground(row % 2 == 0 ? UiTheme.SURFACE : UiTheme.SURFACE_ALT);
                component.setForeground(UiTheme.INK);
            }
            setBorder(UiTheme.empty(0, 8, 0, 8));
            return component;
        }
    }
}
