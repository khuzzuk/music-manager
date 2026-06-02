package pl.khuzzuk.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.nio.file.Path;

class IndexDirectoriesDialogModeler {
    void modelDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(UiTheme.BACKGROUND);
        dialog.setMinimumSize(new Dimension(520, 320));
    }

    void modelPathsList(JList<Path> list) {
        list.setFont(UiTheme.BODY_FONT);
        list.setForeground(UiTheme.INK);
        list.setBackground(UiTheme.SURFACE);
        list.setSelectionBackground(UiTheme.SELECTION);
        list.setSelectionForeground(UiTheme.INK);
        list.setFixedCellHeight(30);
        list.setBorder(UiTheme.empty(6, 8, 6, 8));
    }

    void modelScrollPane(JScrollPane scrollPane) {
        UiTheme.modelScrollPane(scrollPane);
    }

    void modelButtonPanel(JPanel panel) {
        panel.setBackground(UiTheme.BACKGROUND);
        panel.setBorder(UiTheme.empty(8, 12, 12, 12));
    }

    void modelAddButton(JButton button) {
        UiTheme.modelPrimaryButton(button);
    }
}
