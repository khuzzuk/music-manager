package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.RatingIcon;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;

class RatingEditorModeler {
    void modelEditor(JComponent editor, int maxRating, int padding) {
        RatingIcon icon = new RatingIcon(maxRating);
        editor.setPreferredSize(new Dimension(icon.getIconWidth() + padding * 2, icon.getIconHeight() + padding * 2));
        editor.setFocusable(true);
        editor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
