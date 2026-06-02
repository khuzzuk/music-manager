package pl.khuzzuk.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;

class MainWindowModeler {
    void modelWindow(JFrame window) {
        UiTheme.installDefaults();
        window.getContentPane().setBackground(UiTheme.BACKGROUND);
    }

    void modelPlayerPaneContainer(JPanel container) {
        container.setOpaque(false);
        container.setBackground(UiTheme.BACKGROUND);
        container.setBorder(UiTheme.empty(0, 10, 10, 10));
    }
}
