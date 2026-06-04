package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;

class PlayerPaneModeler {
    private static final Color PLAYER_BACKGROUND = UiTheme.DARK_PANEL;
    private static final Color PLAYER_BACKGROUND_ALT = UiTheme.DARK_PANEL_ALT;
    private static final Color PLAYER_PANEL_TOP = new Color(35, 37, 40);
    private static final Color PLAYER_PANEL_BOTTOM = new Color(18, 20, 23);
    private static final Color PLAYER_PANEL_EDGE = new Color(78, 70, 57);
    private static final Color PLAYER_PANEL_HIGHLIGHT = new Color(255, 246, 224, 34);
    private static final Color PLAYER_PANEL_SHADOW = new Color(0, 0, 0, 76);
    private static final Color TRACK_REST = new Color(56, 55, 52);
    private static final Color TRACK_HALO = new Color(96, 76, 42);
    private static final Color THUMB_FILL = new Color(239, 231, 213);
    private static final Dimension PROGRESS_THUMB_SIZE = new Dimension(16, 16);
    private static final Dimension VOLUME_THUMB_SIZE = new Dimension(13, 19);
    private static final int TRACK_SIZE = 6;
    void modelPane(JPanel pane) {
        pane.setOpaque(false);
        pane.setBackground(PLAYER_BACKGROUND);
        pane.setBorder(BorderFactory.createCompoundBorder(
                new ArchivePanelBorder(),
                BorderFactory.createEmptyBorder(13, 17, 13, 17)));
    }

    void modelControlsPanel(JPanel panel) {
        panel.setOpaque(false);
    }

    void modelStatusPanel(JPanel panel) {
        panel.setOpaque(false);
    }

    void modelVolumePanel(JPanel panel) {
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
    }

    void modelProgressSlider(JSlider progressSlider) {
        progressSlider.setPaintTicks(false);
        progressSlider.setPaintLabels(false);
        progressSlider.setFocusable(false);
        progressSlider.setOpaque(false);
        progressSlider.setForeground(UiTheme.ACCENT);
        progressSlider.setBackground(PLAYER_BACKGROUND);
        progressSlider.setUI(new PlayerSliderUi(progressSlider, PROGRESS_THUMB_SIZE, true));
        progressSlider.setBorder(BorderFactory.createEmptyBorder(19, 10, 19, 10));
    }

    void modelVolumeSlider(JSlider volumeSlider) {
        volumeSlider.setFocusable(false);
        volumeSlider.setPaintTicks(false);
        volumeSlider.setPaintLabels(false);
        volumeSlider.setOpaque(false);
        volumeSlider.setForeground(UiTheme.SELECTION_STRONG);
        volumeSlider.setBackground(PLAYER_BACKGROUND);
        volumeSlider.setUI(new PlayerSliderUi(volumeSlider, VOLUME_THUMB_SIZE, false));
        volumeSlider.setPreferredSize(new Dimension(38, 116));
        volumeSlider.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 12));
    }

    void modelIconButton(JButton button) {
        button.setFont(UiTheme.SYMBOL_FONT);
        button.setForeground(UiTheme.LIGHT_TEXT);
        button.setBackground(PLAYER_BACKGROUND_ALT);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setUI(new ArchiveButtonUi());
        button.setBorder(new ArchiveButtonBorder());
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(48, 36));
        button.setMinimumSize(new Dimension(48, 36));
        button.setMargin(new Insets(0, 0, 0, 0));
    }

    void modelTimeLabel(JLabel label) {
        label.setFont(UiTheme.MONO_FONT);
        label.setForeground(UiTheme.LIGHT_TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(52, 20));
    }

    void modelTimeSeparator(JLabel label) {
        label.setFont(UiTheme.MONO_FONT);
        label.setForeground(UiTheme.SELECTION_STRONG);
    }

    private static class PlayerSliderUi extends BasicSliderUI {
        private final Dimension thumbSize;
        private final boolean prominent;

        private PlayerSliderUi(JSlider slider, Dimension thumbSize, boolean prominent) {
            super(slider);
            this.thumbSize = thumbSize;
            this.prominent = prominent;
        }

        @Override
        protected Dimension getThumbSize() {
            return thumbSize;
        }

        @Override
        public void paintTrack(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle track = trackRect;
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                int centerY = track.y + track.height / 2;
                int trackY = centerY - TRACK_SIZE / 2;
                int fillWidth = thumbRect.x + thumbRect.width / 2 - track.x;
                paintRoundTrack(graphics2D, track.x, trackY - 1, track.width, TRACK_SIZE + 2, TRACK_HALO);
                paintRoundTrack(graphics2D, track.x, trackY, track.width, TRACK_SIZE, TRACK_REST);
                if (fillWidth > 0) {
                    paintRoundTrack(graphics2D, track.x, trackY, fillWidth, TRACK_SIZE, UiTheme.SELECTION_STRONG);
                }
            } else {
                int centerX = track.x + track.width / 2;
                int trackX = centerX - TRACK_SIZE / 2;
                int thumbCenterY = thumbRect.y + thumbRect.height / 2;
                int fillHeight = track.y + track.height - thumbCenterY;
                paintRoundTrack(graphics2D, trackX - 1, track.y, TRACK_SIZE + 2, track.height, TRACK_HALO);
                paintRoundTrack(graphics2D, trackX, track.y, TRACK_SIZE, track.height, TRACK_REST);
                if (fillHeight > 0) {
                    paintRoundTrack(graphics2D, trackX, thumbCenterY, TRACK_SIZE, fillHeight, UiTheme.SELECTION_STRONG);
                }
            }

            graphics2D.dispose();
        }

        @Override
        public void paintThumb(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics2D.setColor(new Color(0, 0, 0, 85));
            graphics2D.fillRoundRect(thumbRect.x + 1, thumbRect.y + 2, thumbRect.width, thumbRect.height, 10, 10);
            graphics2D.setPaint(new GradientPaint(
                    thumbRect.x,
                    thumbRect.y,
                    prominent ? new Color(255, 249, 232) : new Color(228, 218, 195),
                    thumbRect.x,
                    thumbRect.y + thumbRect.height,
                    prominent ? THUMB_FILL.darker() : new Color(183, 168, 134)));
            graphics2D.fillRoundRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height, 10, 10);
            graphics2D.setColor(new Color(255, 255, 255, 96));
            graphics2D.drawLine(thumbRect.x + 3, thumbRect.y + 3, thumbRect.x + thumbRect.width - 4, thumbRect.y + 3);
            graphics2D.setColor(UiTheme.SELECTION_STRONG);
            graphics2D.drawRoundRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1, 10, 10);

            graphics2D.dispose();
        }

        @Override
        public void paintFocus(Graphics graphics) {
            // Sliders in the player are controlled directly and do not draw a focus ring.
        }

        private void paintRoundTrack(Graphics2D graphics2D, int x, int y, int width, int height, Color color) {
            graphics2D.setColor(color);
            graphics2D.fillRoundRect(x, y, width, height, TRACK_SIZE, TRACK_SIZE);
        }
    }

    private static class ArchiveButtonBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(2, 2, 3, 2);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setStroke(new BasicStroke(1f));
            graphics2D.setColor(new Color(0, 0, 0, 110));
            graphics2D.drawRoundRect(
                    x + 1,
                    y + 2,
                    width - 3,
                    height - 5,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setColor(UiTheme.DARK_PANEL_LINE);
            graphics2D.drawRoundRect(
                    x,
                    y,
                    width - 2,
                    height - 3,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setColor(UiTheme.ARCHIVE_GOLD_DARK);
            graphics2D.drawLine(x + 9, y + height - 3, x + width - 10, y + height - 3);
            graphics2D.dispose();
        }
    }

    private static class ArchiveButtonUi extends BasicButtonUI {
        @Override
        public void paint(Graphics graphics, javax.swing.JComponent component) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = component.getWidth();
            int height = component.getHeight();
            boolean pressed = component instanceof JButton button && button.getModel().isPressed();
            int yOffset = pressed ? 1 : 0;
            graphics2D.setColor(new Color(0, 0, 0, pressed ? 95 : 125));
            graphics2D.fillRoundRect(
                    3,
                    4,
                    width - 6,
                    height - 7,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setPaint(new GradientPaint(
                    0,
                    yOffset,
                    pressed ? new Color(24, 26, 29) : new Color(42, 44, 48),
                    0,
                    height,
                    pressed ? new Color(34, 36, 40) : new Color(25, 27, 30)));
            graphics2D.fillRoundRect(
                    1,
                    yOffset,
                    width - 3,
                    height - 4,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setColor(new Color(UiTheme.ARCHIVE_BURGUNDY.getRed(), UiTheme.ARCHIVE_BURGUNDY.getGreen(), UiTheme.ARCHIVE_BURGUNDY.getBlue(), pressed ? 76 : 48));
            graphics2D.drawLine(8, height - 5, width - 9, height - 5);
            graphics2D.dispose();

            super.paint(graphics, component);
        }
    }

    private static class ArchivePanelBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 5, 1);
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(PLAYER_PANEL_SHADOW);
            graphics2D.fillRoundRect(
                    x + 3,
                    y + 5,
                    width - 6,
                    height - 8,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setPaint(new GradientPaint(x, y, PLAYER_PANEL_TOP, x, y + height, PLAYER_PANEL_BOTTOM));
            graphics2D.fillRoundRect(
                    x,
                    y,
                    width - 2,
                    height - 6,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setColor(PLAYER_PANEL_HIGHLIGHT);
            graphics2D.drawLine(
                    x + UiTheme.CORNER_RADIUS,
                    y + 1,
                    x + width - UiTheme.CORNER_RADIUS - 2,
                    y + 1);
            graphics2D.setColor(PLAYER_PANEL_EDGE);
            graphics2D.drawRoundRect(
                    x,
                    y,
                    width - 2,
                    height - 6,
                    UiTheme.CORNER_RADIUS,
                    UiTheme.CORNER_RADIUS);
            graphics2D.setColor(UiTheme.ARCHIVE_GOLD_DARK);
            graphics2D.drawLine(
                    x + UiTheme.CORNER_RADIUS,
                    y + height - 7,
                    x + width - UiTheme.CORNER_RADIUS - 2,
                    y + height - 7);
            graphics2D.dispose();
        }
    }
}
