package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.RatingIcon;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;

public class RatingCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table,
                "",
                isSelected,
                hasFocus,
                row,
                column);
        int rating = value instanceof Integer integer ? integer : 0;
        label.setIcon(new RatingIcon(rating));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}
