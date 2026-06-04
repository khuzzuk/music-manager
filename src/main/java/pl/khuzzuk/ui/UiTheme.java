package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Component;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class UiTheme {
    public static final Color BACKGROUND = new Color(238, 235, 228);
    public static final Color SURFACE = new Color(250, 248, 243);
    public static final Color SURFACE_ALT = new Color(229, 225, 216);
    public static final Color INK = new Color(28, 29, 31);
    public static final Color MUTED_INK = new Color(89, 87, 82);
    public static final Color ACCENT = new Color(169, 137, 77);
    public static final Color ACCENT_DARK = new Color(107, 78, 42);
    public static final Color SELECTION = new Color(219, 206, 173);
    public static final Color SELECTION_STRONG = new Color(196, 160, 91);
    public static final Color BORDER = new Color(190, 181, 162);
    public static final Color DARK_PANEL = new Color(24, 26, 29);
    public static final Color DARK_PANEL_ALT = new Color(34, 37, 41);
    public static final Color LIGHT_TEXT = new Color(244, 238, 224);
    public static final Color ARCHIVE_GOLD_DARK = new Color(122, 91, 48);
    public static final Color ARCHIVE_BURGUNDY = new Color(95, 42, 50);
    public static final Color DARK_PANEL_LINE = new Color(69, 64, 56);
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color DIRECTORY_TAB = new Color(193, 156, 91);
    public static final Color DIRECTORY_TAB_OPEN = new Color(211, 176, 111);
    public static final Color DIRECTORY_BODY = new Color(244, 231, 203);
    public static final Color DIRECTORY_BODY_OPEN = new Color(250, 239, 214);
    public static final Color DIRECTORY_HIGHLIGHT = new Color(255, 252, 246, 135);
    public static final Color RATING_EMPTY = new Color(176, 162, 137);
    public static final int CORNER_RADIUS = 14;
    private static final int SCROLLBAR_WIDTH = 10;

    static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font BODY_BOLD_FONT = new Font("Segoe UI", Font.BOLD, 13);
    static final Font TITLE_FONT = new Font("Georgia", Font.BOLD, 16);
    static final Font MONO_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    static final Font SYMBOL_FONT = new Font("Segoe UI Symbol", Font.PLAIN, 15);

    private UiTheme() {
    }

    static void installDefaults() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.font", BODY_BOLD_FONT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.select", SELECTION);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.caretForeground", ACCENT_DARK);
        UIManager.put("TextField.border", lineBorder());
        UIManager.put("Table.font", BODY_FONT);
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("TableHeader.font", BODY_BOLD_FONT);
        UIManager.put("MenuBar.background", DARK_PANEL);
        UIManager.put("MenuBar.foreground", LIGHT_TEXT);
        UIManager.put("Menu.background", DARK_PANEL);
        UIManager.put("Menu.foreground", LIGHT_TEXT);
        UIManager.put("MenuItem.background", SURFACE);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("ScrollPane.border", lineBorder());
    }

    static Border lineBorder() {
        return BorderFactory.createLineBorder(BORDER);
    }

    static Border roundedLineBorder() {
        return new RoundedLineBorder(BORDER, CORNER_RADIUS);
    }

    static Border roundedPanelPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                roundedLineBorder(),
                BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    static Border empty(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    static void modelPrimaryButton(JButton button) {
        button.setFont(BODY_BOLD_FONT);
        button.setForeground(LIGHT_TEXT);
        button.setBackground(ACCENT_DARK);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void modelSecondaryButton(JButton button) {
        button.setFont(BODY_FONT);
        button.setForeground(INK);
        button.setBackground(SURFACE);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                lineBorder(),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static void modelScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(lineBorder());
        scrollPane.getViewport().setBackground(SURFACE);
        modelScrollBar(scrollPane.getVerticalScrollBar());
        modelScrollBar(scrollPane.getHorizontalScrollBar());
    }

    static void modelRoundedScrollPane(JScrollPane scrollPane) {
        modelRoundedScrollPane(scrollPane, SURFACE);
    }

    static void modelRoundedScrollPane(JScrollPane scrollPane, Color viewportBackground) {
        scrollPane.setOpaque(false);
        scrollPane.setBackground(TRANSPARENT);
        scrollPane.setBorder(roundedLineBorder());
        scrollPane.setViewportBorder(empty(0, 0, 0, 0));
        installRoundedViewports(scrollPane, viewportBackground);
        modelScrollBar(scrollPane.getVerticalScrollBar());
        modelScrollBar(scrollPane.getHorizontalScrollBar());
    }

    static void modelScrollBar(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        scrollBar.setOpaque(false);
        scrollBar.setBackground(SURFACE);
        scrollBar.setForeground(ACCENT_DARK);
        scrollBar.setPreferredSize(new Dimension(SCROLLBAR_WIDTH, SCROLLBAR_WIDTH));
        scrollBar.setUnitIncrement(12);
        scrollBar.setBlockIncrement(48);
        scrollBar.setUI(new ArchiveScrollBarUi());
    }

    static void modelTable(JTable table) {
        table.setFont(BODY_FONT);
        table.setForeground(INK);
        table.setBackground(SURFACE);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(INK);
        table.setGridColor(new Color(226, 216, 198));
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setFocusable(true);
        table.getTableHeader().setFont(BODY_BOLD_FONT);
        table.getTableHeader().setForeground(ACCENT_DARK);
        table.getTableHeader().setBackground(SURFACE_ALT);
        table.getTableHeader().setBorder(lineBorder());
    }

    static Shape roundedShape(Component component) {
        return new RoundRectangle2D.Double(
                0,
                0,
                component.getWidth(),
                component.getHeight(),
                CORNER_RADIUS,
                CORNER_RADIUS);
    }

    static void paintRoundedBackground(Component component, Graphics graphics, Color background) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(background);
            graphics2D.fill(roundedShape(component));
        } finally {
            graphics2D.dispose();
        }
    }

    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        private RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.top = 1;
            insets.left = 1;
            insets.bottom = 1;
            insets.right = 1;
            return insets;
        }

        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(color);
                graphics2D.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            } finally {
                graphics2D.dispose();
            }
        }
    }

    private static class ArchiveScrollBarUi extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = ACCENT_DARK;
            trackColor = SURFACE_ALT;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, java.awt.Rectangle trackBounds) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(SURFACE_ALT);
                graphics2D.fillRoundRect(
                        trackBounds.x + 2,
                        trackBounds.y + 2,
                        trackBounds.width - 4,
                        trackBounds.height - 4,
                        SCROLLBAR_WIDTH,
                        SCROLLBAR_WIDTH);
            } finally {
                graphics2D.dispose();
            }
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, java.awt.Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(SELECTION_STRONG);
                graphics2D.fillRoundRect(
                        thumbBounds.x + 2,
                        thumbBounds.y + 2,
                        thumbBounds.width - 4,
                        thumbBounds.height - 4,
                        SCROLLBAR_WIDTH,
                        SCROLLBAR_WIDTH);
                graphics2D.setColor(ACCENT_DARK);
                graphics2D.drawRoundRect(
                        thumbBounds.x + 2,
                        thumbBounds.y + 2,
                        thumbBounds.width - 5,
                        thumbBounds.height - 5,
                        SCROLLBAR_WIDTH,
                        SCROLLBAR_WIDTH);
            } finally {
                graphics2D.dispose();
            }
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }
    }

    private static void installRoundedViewports(JScrollPane scrollPane, Color viewportBackground) {
        JViewport columnHeader = scrollPane.getColumnHeader();
        boolean hasColumnHeader = columnHeader != null && columnHeader.getView() != null;

        scrollPane.setViewport(createRoundedViewport(
                scrollPane.getViewport(),
                viewportBackground,
                !hasColumnHeader,
                true));

        if (hasColumnHeader) {
            columnHeader.setOpaque(false);
            columnHeader.setBackground(TRANSPARENT);
        }
    }

    private static JViewport createRoundedViewport(
            JViewport currentViewport,
            Color background,
            boolean roundTopCorners,
            boolean roundBottomCorners) {
        Component view = currentViewport == null ? null : currentViewport.getView();
        RoundedViewport roundedViewport = new RoundedViewport(background, roundTopCorners, roundBottomCorners);
        if (view != null) {
            roundedViewport.setView(view);
        }
        return roundedViewport;
    }

    private static class RoundedViewport extends JViewport {
        private final boolean roundTopCorners;
        private final boolean roundBottomCorners;

        private RoundedViewport(Color background, boolean roundTopCorners, boolean roundBottomCorners) {
            this.roundTopCorners = roundTopCorners;
            this.roundBottomCorners = roundBottomCorners;
            setOpaque(false);
            setBackground(background);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.setColor(getBackground());
                graphics2D.fill(roundedClip());
            } finally {
                graphics2D.dispose();
            }
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2D.clip(roundedClip());
                super.paintChildren(graphics2D);
            } finally {
                graphics2D.dispose();
            }
        }

        private Shape roundedClip() {
            double width = getWidth();
            double height = getHeight();
            double radius = CORNER_RADIUS;
            Area area = new Area(new RoundRectangle2D.Double(0, 0, width, height, radius, radius));
            if (!roundTopCorners) {
                area.add(new Area(new Rectangle2D.Double(0, 0, width, radius)));
            }
            if (!roundBottomCorners) {
                area.add(new Area(new Rectangle2D.Double(0, height - radius, width, radius)));
            }
            return area;
        }
    }
}
