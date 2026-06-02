package pl.khuzzuk.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.Insets;

class ContentPaneModeler {
    void modelPane(JPanel pane) {
        pane.setBackground(UiTheme.BACKGROUND);
        pane.setBorder(UiTheme.empty(10, 10, 10, 10));
    }

    void modelTracksArea(JPanel tracksArea) {
        tracksArea.setBackground(UiTheme.BACKGROUND);
        tracksArea.setBorder(UiTheme.lineBorder());
    }

    void modelFileBrowserPane(JPanel fileBrowserPane) {
        fileBrowserPane.setOpaque(false);
        fileBrowserPane.setBackground(UiTheme.BACKGROUND);
    }

    void modelScrollPane(JScrollPane scrollPane) {
        UiTheme.modelScrollPane(scrollPane);
    }

    Insets contentInsets() {
        return new Insets(0, 4, 0, 4);
    }

    Insets fileTreeInsets() {
        return new Insets(0, 0, 4, 0);
    }

    Insets tracksFilterInsets() {
        return new Insets(4, 0, 0, 0);
    }

    Insets progressLabelInsets() {
        return new Insets(0, 20, 8, 20);
    }

    void modelProgressPanel(JPanel progressPanel) {
        progressPanel.setBackground(UiTheme.SURFACE);
    }

    void modelProgressLabel(JLabel label) {
        label.setFont(UiTheme.TITLE_FONT);
        label.setForeground(UiTheme.ACCENT_DARK);
    }

    void modelProgressBar(JProgressBar progressBar) {
        progressBar.setStringPainted(true);
        progressBar.setFont(UiTheme.BODY_BOLD_FONT);
        progressBar.setForeground(UiTheme.ACCENT);
        progressBar.setBackground(UiTheme.SURFACE_ALT);
        progressBar.setBorder(UiTheme.lineBorder());
    }
}
