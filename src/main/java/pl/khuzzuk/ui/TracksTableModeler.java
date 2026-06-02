package pl.khuzzuk.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

class TracksTableModeler {
    void modelTable(JTable table) {
        UiTheme.modelTable(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultRenderer(Object.class, new TrackCellRenderer());
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
