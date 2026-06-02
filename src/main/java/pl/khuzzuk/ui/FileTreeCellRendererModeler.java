package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.DirectoryIcon;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Color;
import java.awt.Component;

class FileTreeCellRendererModeler {
    private static final Icon CLOSED_DIRECTORY_ICON = new DirectoryIcon(false);
    private static final Icon OPEN_DIRECTORY_ICON = new DirectoryIcon(true);
    private static final Color SELECTED_TEXT_COLOR = UiTheme.ACCENT_DARK;
    private static final Color TEXT_COLOR = UiTheme.INK;
    private static final Color TRANSPARENT_SELECTION_COLOR = UiTheme.TRANSPARENT;

    void modelRenderer(DefaultTreeCellRenderer renderer) {
        renderer.setOpaque(false);
        renderer.setClosedIcon(CLOSED_DIRECTORY_ICON);
        renderer.setOpenIcon(OPEN_DIRECTORY_ICON);
        renderer.setLeafIcon(CLOSED_DIRECTORY_ICON);
        renderer.setFont(UiTheme.BODY_FONT);
        renderer.setTextNonSelectionColor(TEXT_COLOR);
        renderer.setBackgroundNonSelectionColor(TRANSPARENT_SELECTION_COLOR);
        renderer.setBackgroundSelectionColor(TRANSPARENT_SELECTION_COLOR);
        renderer.setBorderSelectionColor(TRANSPARENT_SELECTION_COLOR);
    }

    void modelRenderedComponent(DefaultTreeCellRenderer renderer, Component component, boolean expanded) {
        if (component instanceof JComponent jComponent) {
            jComponent.setOpaque(false);
            jComponent.setBackground(TRANSPARENT_SELECTION_COLOR);
        }
        renderer.setTextSelectionColor(SELECTED_TEXT_COLOR);
        renderer.setTextNonSelectionColor(TEXT_COLOR);
        renderer.setIcon(expanded ? OPEN_DIRECTORY_ICON : CLOSED_DIRECTORY_ICON);
    }
}
