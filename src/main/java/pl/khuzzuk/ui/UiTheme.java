package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

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

    static Border panelPadding(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                lineBorder(),
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
}
