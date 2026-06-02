package pl.khuzzuk.ui;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.Dimension;

class MainMenuBarModeler {
    void modelMenuBar(JMenuBar menuBar) {
        menuBar.setBackground(UiTheme.DARK_PANEL);
        menuBar.setBorder(UiTheme.empty(4, 8, 4, 8));
        menuBar.setPreferredSize(new Dimension(menuBar.getPreferredSize().width, 34));
    }

    void modelMenu(JMenu menu) {
        menu.setFont(UiTheme.BODY_BOLD_FONT);
        menu.setForeground(UiTheme.LIGHT_TEXT);
        menu.setBackground(UiTheme.DARK_PANEL);
        menu.setOpaque(true);
        menu.setBorder(UiTheme.empty(4, 8, 4, 8));
    }

    void modelMenuItem(JMenuItem menuItem) {
        menuItem.setFont(UiTheme.BODY_FONT);
        menuItem.setForeground(UiTheme.INK);
        menuItem.setBackground(UiTheme.SURFACE);
    }
}
