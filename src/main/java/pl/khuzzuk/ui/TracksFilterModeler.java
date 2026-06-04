package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.Tag;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;

class TracksFilterModeler {
    private static final int FIELD_HEIGHT = 31;
    private static final int FIELD_ARROW_WIDTH = 32;
    private static final Border FIELD_BORDER = BorderFactory.createCompoundBorder(
            UiTheme.roundedLineBorder(),
            UiTheme.empty(0, 8, 0, 2));
    private static final Border CELL_BORDER = UiTheme.empty(0, 8, 0, 8);

    void modelPanel(JPanel panel) {
        panel.setOpaque(false);
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.roundedPanelPadding(8, 8, 8, 8));
    }

    void modelFieldComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(UiTheme.BODY_FONT);
        comboBox.setForeground(UiTheme.INK);
        comboBox.setBackground(UiTheme.SURFACE);
        comboBox.setBorder(FIELD_BORDER);
        comboBox.setOpaque(true);
        comboBox.setFocusable(false);
        comboBox.setMaximumRowCount(12);
        comboBox.setPreferredSize(new Dimension(comboBox.getPreferredSize().width, FIELD_HEIGHT));
        comboBox.setRenderer(new TagComboBoxRenderer());
        comboBox.setUI(new FilterComboBoxUi());
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void modelValuesList(JList<?> list) {
        list.setFont(UiTheme.BODY_FONT);
        list.setForeground(UiTheme.INK);
        list.setBackground(UiTheme.SURFACE);
        list.setSelectionBackground(UiTheme.SELECTION);
        list.setSelectionForeground(UiTheme.INK);
        list.setFixedCellHeight(24);
        list.setCellRenderer(new ValueListRenderer());
    }

    void modelScrollPane(JScrollPane scrollPane) {
        scrollPane.setOpaque(false);
        scrollPane.setBackground(UiTheme.TRANSPARENT);
        scrollPane.setBorder(UiTheme.empty(0, 0, 0, 0));
        scrollPane.setViewportBorder(UiTheme.empty(0, 0, 0, 0));
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(UiTheme.TRANSPARENT);
        UiTheme.modelScrollBar(scrollPane.getVerticalScrollBar());
        UiTheme.modelScrollBar(scrollPane.getHorizontalScrollBar());
    }

    Insets fieldInsets() {
        return new Insets(0, 0, 6, 0);
    }

    private static class TagComboBoxRenderer extends JLabel implements ListCellRenderer<Object> {
        private TagComboBoxRenderer() {
            setOpaque(true);
            setFont(UiTheme.BODY_FONT);
            setBorder(CELL_BORDER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            setText(value instanceof Tag tag ? tag.label() : "");
            setFont(UiTheme.BODY_FONT);
            setBackground(backgroundColor(index, isSelected));
            setForeground(isSelected || index < 0 ? UiTheme.INK : UiTheme.MUTED_INK);
            return this;
        }

        private Color backgroundColor(int index, boolean isSelected) {
            if (isSelected) {
                return UiTheme.SELECTION;
            }
            return index % 2 == 1 ? UiTheme.SURFACE_ALT : UiTheme.SURFACE;
        }
    }

    private static class FilterComboBoxUi extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            return new FilterArrowButton();
        }

        @Override
        protected ComboPopup createPopup() {
            return new BasicComboPopup(comboBox) {
                @Override
                protected JScrollPane createScroller() {
                    JScrollPane scrollPane = super.createScroller();
                    scrollPane.setBorder(UiTheme.lineBorder());
                    scrollPane.getViewport().setBackground(UiTheme.SURFACE);
                    UiTheme.modelScrollBar(scrollPane.getVerticalScrollBar());
                    UiTheme.modelScrollBar(scrollPane.getHorizontalScrollBar());
                    return scrollPane;
                }
            };
        }
    }

    private static class ValueListRenderer extends JLabel implements ListCellRenderer<Object> {
        private ValueListRenderer() {
            setOpaque(true);
            setFont(UiTheme.BODY_FONT);
            setBorder(CELL_BORDER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            setText(value == null ? "" : value.toString());
            setFont(UiTheme.BODY_FONT);
            setBackground(isSelected ? UiTheme.SELECTION : rowBackground(index));
            setForeground(isSelected ? UiTheme.INK : UiTheme.MUTED_INK);
            return this;
        }

        private Color rowBackground(int index) {
            return index % 2 == 1 ? UiTheme.SURFACE_ALT : UiTheme.SURFACE;
        }
    }

    private static class FilterArrowButton extends JButton {
        private FilterArrowButton() {
            setPreferredSize(new Dimension(FIELD_ARROW_WIDTH, FIELD_HEIGHT));
            setMinimumSize(new Dimension(FIELD_ARROW_WIDTH, FIELD_HEIGHT));
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean pressed = getModel().isPressed();
                boolean rollover = getModel().isRollover();
                int width = getWidth();
                int height = getHeight();
                int yOffset = pressed ? 1 : 0;

                graphics2D.setColor(new Color(0, 0, 0, 38));
                graphics2D.fillRoundRect(
                        4,
                        5 + yOffset,
                        width - 8,
                        height - 10,
                        UiTheme.CORNER_RADIUS,
                        UiTheme.CORNER_RADIUS);
                graphics2D.setPaint(new GradientPaint(
                        0,
                        yOffset,
                        rollover ? new Color(242, 234, 215) : UiTheme.SURFACE_ALT,
                        0,
                        height,
                        pressed ? new Color(198, 187, 162) : new Color(220, 211, 188)));
                graphics2D.fillRoundRect(
                        3,
                        3 + yOffset,
                        width - 7,
                        height - 8,
                        UiTheme.CORNER_RADIUS,
                        UiTheme.CORNER_RADIUS);
                graphics2D.setColor(UiTheme.BORDER);
                graphics2D.drawRoundRect(
                        3,
                        3 + yOffset,
                        width - 8,
                        height - 9,
                        UiTheme.CORNER_RADIUS,
                        UiTheme.CORNER_RADIUS);
                graphics2D.setColor(UiTheme.ACCENT_DARK);
                graphics2D.fillPolygon(chevron(width / 2, height / 2 + yOffset));
            } finally {
                graphics2D.dispose();
            }
        }

        private Polygon chevron(int centerX, int centerY) {
            Polygon polygon = new Polygon();
            polygon.addPoint(centerX - 5, centerY - 2);
            polygon.addPoint(centerX + 5, centerY - 2);
            polygon.addPoint(centerX, centerY + 4);
            return polygon;
        }
    }

}
