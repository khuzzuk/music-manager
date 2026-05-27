package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.index.RootIndexItem;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class FileTree extends JTree {
    public FileTree(IndexReaderService indexReaderService, IndexService indexService) {
        super(createRoot(indexReaderService.getCurrentRootIndexItem()));
        setToggleClickCount(1);
        putClientProperty("JTree.lineStyle", "None");
        UIManager.put("Tree.collapsedIcon", new TreeToggleIcon(false));
        UIManager.put("Tree.expandedIcon", new TreeToggleIcon(true));
        updateUI();
        setCellRenderer(new FileTreeCellRenderer());
        indexService.addIndexListener(root -> SwingUtilities.invokeLater(() -> refresh(root)));
        expandRow(0);
    }

    private void refresh(RootIndexItem rootIndexItem) {
        setModel(new DefaultTreeModel(createRoot(rootIndexItem)));
        expandRow(0);
    }

    private static DefaultMutableTreeNode createRoot(RootIndexItem rootIndexItem) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Muzyka");
        rootIndexItem.getChildren().stream()
                .filter(IndexItem::isDirectory)
                .map(FileTree::createNode)
                .forEach(root::add);
        return root;
    }

    private static DefaultMutableTreeNode createNode(IndexItem item) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(item.getName());
        item.getChildren().stream()
                .filter(IndexItem::isDirectory)
                .map(FileTree::createNode)
                .forEach(node::add);
        return node;
    }
}
