package pl.khuzzuk.ui;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

class PlaylistPaneModeler {
    void modelPane(JScrollPane pane) {
        pane.setBorder(UiTheme.panelPadding(0, 0, 0, 0));
        pane.getViewport().setBackground(UiTheme.DARK_PANEL_ALT);
    }

    void modelPlaylist(JTable playlist) {
        playlist.setFont(UiTheme.BODY_FONT);
        playlist.setForeground(UiTheme.LIGHT_TEXT);
        playlist.setBackground(UiTheme.DARK_PANEL_ALT);
        playlist.setSelectionBackground(UiTheme.ACCENT_DARK);
        playlist.setSelectionForeground(UiTheme.LIGHT_TEXT);
        playlist.setRowHeight(30);
        playlist.setShowGrid(false);
        playlist.setFillsViewportHeight(true);
        playlist.setDefaultRenderer(Object.class, new PlaylistCellRenderer());
    }

    private static class PlaylistCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            component.setFont(column == 0 ? UiTheme.SYMBOL_FONT : UiTheme.BODY_FONT);
            if (selected) {
                component.setBackground(UiTheme.ACCENT_DARK);
                component.setForeground(UiTheme.LIGHT_TEXT);
            } else {
                component.setBackground(row % 2 == 0 ? UiTheme.DARK_PANEL : UiTheme.DARK_PANEL_ALT);
                component.setForeground(column == 0 ? UiTheme.SELECTION_STRONG : UiTheme.LIGHT_TEXT);
            }
            setBorder(UiTheme.empty(0, 8, 0, 8));
            return component;
        }
    }
}
