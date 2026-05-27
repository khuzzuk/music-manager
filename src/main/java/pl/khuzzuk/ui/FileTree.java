package pl.khuzzuk.ui;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class FileTree extends JTree {
    public FileTree() {
        super(createRoot());
    }

    private static DefaultMutableTreeNode createRoot() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Muzyka");
        root.add(new DefaultMutableTreeNode("1"));
        root.add(new DefaultMutableTreeNode("2"));
        return root;
    }
}
