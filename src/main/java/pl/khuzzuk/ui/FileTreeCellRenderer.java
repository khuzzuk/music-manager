package pl.khuzzuk.ui;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    private final FileTreeCellRendererModeler modeler = new FileTreeCellRendererModeler();

    FileTreeCellRenderer() {
        modeler.modelRenderer(this);
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
        modeler.modelRenderedComponent(this, component, expanded);
        return component;
    }
}
