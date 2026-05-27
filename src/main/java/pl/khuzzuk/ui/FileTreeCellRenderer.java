package pl.khuzzuk.ui;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Icon CLOSED_DIRECTORY_ICON = new DirectoryIcon(false);
    private static final Icon OPEN_DIRECTORY_ICON = new DirectoryIcon(true);

    FileTreeCellRenderer() {
        setClosedIcon(CLOSED_DIRECTORY_ICON);
        setOpenIcon(OPEN_DIRECTORY_ICON);
        setLeafIcon(CLOSED_DIRECTORY_ICON);
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        if (component instanceof JComponent jComponent) {
            jComponent.setOpaque(selected);
        }
        setIcon(expanded ? OPEN_DIRECTORY_ICON : CLOSED_DIRECTORY_ICON);
        return component;
    }
}
