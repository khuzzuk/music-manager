package pl.khuzzuk.ui;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Insets;

class TracksFilterModeler {
    void modelPanel(JPanel panel) {
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.panelPadding(8, 8, 8, 8));
    }

    void modelFieldComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(UiTheme.BODY_FONT);
        comboBox.setForeground(UiTheme.INK);
        comboBox.setBackground(UiTheme.SURFACE);
        comboBox.setFocusable(false);
    }

    void modelValuesList(JList<?> list) {
        list.setFont(UiTheme.BODY_FONT);
        list.setForeground(UiTheme.INK);
        list.setBackground(UiTheme.SURFACE);
        list.setSelectionBackground(UiTheme.SELECTION);
        list.setSelectionForeground(UiTheme.INK);
        list.setFixedCellHeight(24);
    }

    void modelScrollPane(JScrollPane scrollPane) {
        UiTheme.modelScrollPane(scrollPane);
    }

    Insets fieldInsets() {
        return new Insets(0, 0, 6, 0);
    }
}
