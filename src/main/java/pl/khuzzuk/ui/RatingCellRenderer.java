package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.RatingIcon;

import javax.swing.JLabel;
import javax.swing.JTable;
import java.awt.Component;

public class RatingCellRenderer extends TracksTableCellRenderer {
    private final RatingCellRendererModeler modeler = new RatingCellRendererModeler();

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
        modeler.modelRatingLabel(label, table, isSelected, row);
        return label;
    }
}
