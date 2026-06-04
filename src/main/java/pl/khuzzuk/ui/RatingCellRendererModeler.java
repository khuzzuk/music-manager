package pl.khuzzuk.ui;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

class RatingCellRendererModeler {
    void modelRatingLabel(JLabel label, JTable table, boolean selected, int row) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(UiTheme.BODY_FONT);
        label.setForeground(UiTheme.ACCENT_DARK);
        label.setOpaque(false);
        if (selected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(row % 2 == 0 ? UiTheme.SURFACE : UiTheme.SURFACE_ALT);
            label.setForeground(UiTheme.ACCENT_DARK);
        }
    }
}
