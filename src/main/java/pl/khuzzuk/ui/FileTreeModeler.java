package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.TreeToggleIcon;

import javax.swing.JTree;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

class FileTreeModeler {
    private static final Color SELECTION_BACKGROUND = UiTheme.SELECTION;

    void modelTree(JTree tree) {
        tree.setOpaque(false);
        tree.setBackground(UiTheme.SURFACE);
        tree.setForeground(UiTheme.INK);
        tree.setFont(UiTheme.BODY_FONT);
        tree.setRowHeight(28);
        tree.setBorder(UiTheme.panelPadding(8, 6, 8, 6));
        tree.putClientProperty("JTree.lineStyle", "None");
        UIManager.put("Tree.collapsedIcon", new TreeToggleIcon(false));
        UIManager.put("Tree.expandedIcon", new TreeToggleIcon(true));
    }

    void paintSelectionRow(Graphics graphics, Rectangle rowBounds, int width) {
        graphics.setColor(SELECTION_BACKGROUND);
        graphics.fillRect(0, rowBounds.y, width, rowBounds.height);
    }
}
