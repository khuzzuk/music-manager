package pl.khuzzuk.ui;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ButtonModel;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

class SavedPlaylistsPaneModeler {
    private static final int BUTTON_RADIUS = UiTheme.CORNER_RADIUS;
    private static final int BUTTON_HEIGHT = 32;
    private static final int BUTTON_MIN_WIDTH = 92;
    private static final Color SAVE_BACKGROUND = UiTheme.ACCENT_DARK;
    private static final Color SAVE_HOVER_BACKGROUND = UiTheme.ARCHIVE_GOLD_DARK;
    private static final Color SAVE_PRESSED_BACKGROUND = new Color(83, 59, 31);
    private static final Color LOAD_BACKGROUND = UiTheme.DARK_PANEL_ALT;
    private static final Color LOAD_HOVER_BACKGROUND = new Color(46, 50, 55);
    private static final Color LOAD_PRESSED_BACKGROUND = UiTheme.DARK_PANEL;

    void modelPane(JPanel pane) {
        pane.setOpaque(false);
        pane.setBackground(UiTheme.DARK_PANEL);
        pane.setBorder(UiTheme.roundedPanelPadding(8, 8, 8, 8));
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
        UiTheme.modelRoundedScrollPane(scrollPane, UiTheme.DARK_PANEL_ALT);
    }

    void modelButtonsPanel(JPanel panel) {
        panel.setOpaque(false);
        panel.setBackground(UiTheme.DARK_PANEL);
    }

    void modelSaveButton(JButton button) {
        modelPlaylistButton(
                button,
                SAVE_BACKGROUND,
                SAVE_HOVER_BACKGROUND,
                SAVE_PRESSED_BACKGROUND,
                UiTheme.ACCENT,
                UiTheme.LIGHT_TEXT);
    }

    void modelLoadButton(JButton button) {
        modelPlaylistButton(
                button,
                LOAD_BACKGROUND,
                LOAD_HOVER_BACKGROUND,
                LOAD_PRESSED_BACKGROUND,
                UiTheme.DARK_PANEL_LINE,
                UiTheme.LIGHT_TEXT);
    }

    private void modelPlaylistButton(
            JButton button,
            Color background,
            Color hoverBackground,
            Color pressedBackground,
            Color border,
            Color foreground) {
        button.setFont(UiTheme.BODY_BOLD_FONT);
        button.setForeground(foreground);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(UiTheme.empty(7, 14, 7, 14));
        button.setPreferredSize(new Dimension(
                Math.max(BUTTON_MIN_WIDTH, button.getPreferredSize().width + 20),
                BUTTON_HEIGHT));
        button.setUI(new PlaylistButtonUi(background, hoverBackground, pressedBackground, border));
    }

    private static class PlaylistButtonUi extends BasicButtonUI {
        private final Color background;
        private final Color hoverBackground;
        private final Color pressedBackground;
        private final Color border;

        private PlaylistButtonUi(Color background, Color hoverBackground, Color pressedBackground, Color border) {
            this.background = background;
            this.hoverBackground = hoverBackground;
            this.pressedBackground = pressedBackground;
            this.border = border;
        }

        @Override
        public void paint(Graphics graphics, javax.swing.JComponent component) {
            AbstractButton button = (AbstractButton) component;
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = button.getModel();
                graphics2D.setColor(buttonBackground(model));
                graphics2D.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), BUTTON_RADIUS, BUTTON_RADIUS);
                graphics2D.setColor(border);
                graphics2D.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, BUTTON_RADIUS, BUTTON_RADIUS);
            } finally {
                graphics2D.dispose();
            }
            super.paint(graphics, component);
        }

        private Color buttonBackground(ButtonModel model) {
            if (model.isPressed() || model.isArmed()) {
                return pressedBackground;
            }
            if (model.isRollover()) {
                return hoverBackground;
            }
            return background;
        }
    }
}
