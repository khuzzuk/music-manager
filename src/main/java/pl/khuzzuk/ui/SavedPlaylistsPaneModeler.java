package pl.khuzzuk.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class SavedPlaylistsPaneModeler {
    void modelPane(JPanel pane) {
        pane.setBackground(UiTheme.DARK_PANEL);
        pane.setBorder(UiTheme.panelPadding(8, 8, 8, 8));
    }

    void modelTitle(JLabel title) {
        title.setFont(UiTheme.BODY_BOLD_FONT);
        title.setForeground(UiTheme.LIGHT_TEXT);
        title.setBorder(UiTheme.empty(0, 2, 2, 2));
    }

    void modelList(JList<?> list) {
        list.setFont(UiTheme.BODY_FONT);
        list.setForeground(UiTheme.LIGHT_TEXT);
        list.setBackground(UiTheme.DARK_PANEL_ALT);
        list.setSelectionBackground(UiTheme.ACCENT_DARK);
        list.setSelectionForeground(UiTheme.LIGHT_TEXT);
        list.setFixedCellHeight(30);
        list.setVisibleRowCount(5);
    }

    void modelScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(UiTheme.lineBorder());
        scrollPane.getViewport().setBackground(UiTheme.DARK_PANEL_ALT);
    }

    void modelButtonsPanel(JPanel panel) {
        panel.setOpaque(false);
        panel.setBackground(UiTheme.DARK_PANEL);
    }

    void modelSaveButton(JButton button) {
        UiTheme.modelPrimaryButton(button);
    }

    void modelLoadButton(JButton button) {
        UiTheme.modelSecondaryButton(button);
    }
}
