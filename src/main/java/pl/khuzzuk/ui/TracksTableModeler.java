package pl.khuzzuk.ui;

import javax.swing.JTable;
import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.Icon;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.util.List;

class TracksTableModeler {
    private static final Color HEADER_BACKGROUND = UiTheme.DARK_PANEL_ALT;
    private static final Color HEADER_FOREGROUND = UiTheme.SELECTION_STRONG;
    private static final Color HEADER_BORDER = UiTheme.DARK_PANEL_LINE;
    private static final Color EDITOR_BACKGROUND = new Color(255, 252, 246);
    private static final Color EDITOR_BORDER = UiTheme.SELECTION_STRONG;
    private static final int HEADER_RADIUS = UiTheme.CORNER_RADIUS;
    private static final int HEADER_HORIZONTAL_INSET = 3;
    private static final int HEADER_VERTICAL_INSET = 3;
    private static final int HEADER_LEFT_GAP = 4;
    private static final int COLUMN_MENU_CHECK_SIZE = 14;
    private static final int EDITOR_RADIUS = 8;

    void modelTable(JTable table) {
        UiTheme.modelTable(table);
        table.setOpaque(false);
        table.setBackground(UiTheme.TRANSPARENT);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Object.class, new TracksTableCellRenderer());
        table.setDefaultEditor(Object.class, new TrackCellEditor());
        table.setDefaultEditor(Integer.class, new TrackCellEditor());
        modelHeader(table.getTableHeader());
    }

    void modelColumnMenu(JPopupMenu menu) {
        menu.setBackground(UiTheme.SURFACE);
        menu.setBorder(UiTheme.lineBorder());
        menu.setOpaque(true);
    }

    void modelContextMenu(JPopupMenu menu) {
        menu.setBackground(UiTheme.SURFACE);
        menu.setBorder(UiTheme.lineBorder());
    }

    void modelColumnMenuItem(JCheckBoxMenuItem item) {
        item.setFont(UiTheme.BODY_FONT);
        item.setForeground(UiTheme.INK);
        item.setBackground(UiTheme.SURFACE);
        item.setOpaque(true);
        item.setBorder(UiTheme.empty(6, 9, 6, 12));
        item.setIconTextGap(9);
        item.setUI(new ColumnMenuItemUi());
    }

    void modelContextMenuItem(JMenuItem item) {
        item.setFont(UiTheme.BODY_FONT);
        item.setForeground(UiTheme.INK);
        item.setBackground(UiTheme.SURFACE);
        item.setBorder(UiTheme.empty(5, 10, 5, 10));
    }

    private void modelHeader(JTableHeader header) {
        header.setDefaultRenderer(new TrackHeaderRenderer());
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);
        header.setBackground(UiTheme.TRANSPARENT);
        header.setForeground(HEADER_FOREGROUND);
        header.setOpaque(true);
        header.setBorder(UiTheme.empty(0, 0, 0, 0));
    }

    private static class TrackHeaderRenderer extends JLabel implements TableCellRenderer {
        private boolean firstColumn;

        private TrackHeaderRenderer() {
            setOpaque(false);
            setFont(UiTheme.BODY_BOLD_FONT);
            setForeground(HEADER_FOREGROUND);
            setHorizontalAlignment(JLabel.LEFT);
            setHorizontalTextPosition(JLabel.LEFT);
            setIconTextGap(7);
            setBorder(UiTheme.empty(0, 10, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            setText(value == null ? "" : value.toString().toUpperCase());
            setIcon(sortIcon(table, column));
            firstColumn = column == 0;
            setBorder(UiTheme.empty(0, firstColumn ? 12 : 10, 0, 8));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = headerX();
                int y = HEADER_VERTICAL_INSET;
                int width = getWidth() - x - HEADER_HORIZONTAL_INSET;
                int height = getHeight() - HEADER_VERTICAL_INSET * 2;
                graphics2D.setColor(HEADER_BACKGROUND);
                graphics2D.fillRoundRect(x, y, width, height, HEADER_RADIUS, HEADER_RADIUS);
                graphics2D.setColor(HEADER_BORDER);
                graphics2D.drawRoundRect(x, y, width - 1, height - 1, HEADER_RADIUS, HEADER_RADIUS);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }

        private int headerX() {
            return firstColumn ? HEADER_LEFT_GAP : HEADER_HORIZONTAL_INSET;
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

    private static class ColumnMenuItemUi extends BasicCheckBoxMenuItemUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            checkIcon = new ColumnMenuCheckIcon();
            selectionBackground = UiTheme.SELECTION;
            selectionForeground = UiTheme.INK;
            disabledForeground = UiTheme.MUTED_INK;
        }
    }

    private static class ColumnMenuCheckIcon implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            boolean selected = component instanceof AbstractButton button && button.isSelected();
            boolean enabled = component == null || component.isEnabled();
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(selected && enabled ? UiTheme.SELECTION_STRONG : UiTheme.SURFACE_ALT);
                graphics2D.fillRoundRect(x, y, COLUMN_MENU_CHECK_SIZE, COLUMN_MENU_CHECK_SIZE, 5, 5);
                graphics2D.setColor(enabled ? UiTheme.ACCENT_DARK : UiTheme.BORDER);
                graphics2D.drawRoundRect(x, y, COLUMN_MENU_CHECK_SIZE - 1, COLUMN_MENU_CHECK_SIZE - 1, 5, 5);

                if (selected) {
                    Stroke previousStroke = graphics2D.getStroke();
                    graphics2D.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics2D.setColor(enabled ? UiTheme.LIGHT_TEXT : UiTheme.MUTED_INK);
                    graphics2D.drawLine(x + 4, y + 7, x + 6, y + 10);
                    graphics2D.drawLine(x + 6, y + 10, x + 11, y + 4);
                    graphics2D.setStroke(previousStroke);
                }
            } finally {
                graphics2D.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return COLUMN_MENU_CHECK_SIZE;
        }

        @Override
        public int getIconHeight() {
            return COLUMN_MENU_CHECK_SIZE;
        }
    }

    private static class TrackCellEditor extends DefaultCellEditor {
        private TrackCellEditor() {
            super(new TrackCellEditorField());
            setClickCountToStart(2);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean selected,
                int row,
                int column) {
            Component component = super.getTableCellEditorComponent(table, value, selected, row, column);
            if (component instanceof TrackCellEditorField editorField) {
                editorField.setText(value == null ? "" : value.toString());
                editorField.selectAll();
            }
            return component;
        }
    }

    private static class TrackCellEditorField extends JTextField {
        private TrackCellEditorField() {
            setOpaque(false);
            setFont(UiTheme.BODY_FONT);
            setForeground(UiTheme.INK);
            setCaretColor(UiTheme.ACCENT_DARK);
            setSelectionColor(UiTheme.SELECTION);
            setSelectedTextColor(UiTheme.INK);
            setBorder(UiTheme.empty(0, 10, 0, 10));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int x = 2;
                int y = 1;
                int width = getWidth() - 4;
                int height = getHeight() - 2;
                graphics2D.setColor(EDITOR_BACKGROUND);
                graphics2D.fillRoundRect(x, y, width, height, EDITOR_RADIUS, EDITOR_RADIUS);
                graphics2D.setColor(EDITOR_BORDER);
                graphics2D.drawRoundRect(x, y, width - 1, height - 1, EDITOR_RADIUS, EDITOR_RADIUS);
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }
    }

}
