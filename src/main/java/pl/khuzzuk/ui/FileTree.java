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
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileTree extends JTree {
    private static final Color SELECTION_BACKGROUND = new Color(218, 235, 252);
    private final Consumer<List<IndexItem>> selectedFilesConsumer;

    public FileTree(
            IndexReaderService indexReaderService,
            IndexService indexService,
            Consumer<List<IndexItem>> selectedFilesConsumer) {
        super(createRoot(indexReaderService.getCurrentRootIndexItem()));
        this.selectedFilesConsumer = selectedFilesConsumer;
        setOpaque(false);
        setToggleClickCount(1);
        putClientProperty("JTree.lineStyle", "None");
        UIManager.put("Tree.collapsedIcon", new TreeToggleIcon(false));
        UIManager.put("Tree.expandedIcon", new TreeToggleIcon(true));
        updateUI();
        setCellRenderer(new FileTreeCellRenderer());
        addMouseListener(new FileTreeSelectionListener());
        indexService.addIndexListener(root -> SwingUtilities.invokeLater(() -> refresh(root)));
        expandRow(0);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());
        paintSelectionRows(graphics);
        super.paintComponent(graphics);
    }

    private void paintSelectionRows(Graphics graphics) {
        int[] selectedRows = getSelectionRows();
        if (selectedRows == null) {
            return;
        }

        graphics.setColor(SELECTION_BACKGROUND);
        for (int selectedRow : selectedRows) {
            Rectangle rowBounds = getRowBounds(selectedRow);
            if (rowBounds != null) {
                graphics.fillRect(0, rowBounds.y, getWidth(), rowBounds.height);
            }
        }
    }

    private void refresh(RootIndexItem rootIndexItem) {
        setModel(new DefaultTreeModel(createRoot(rootIndexItem)));
        expandRow(0);
    }

    private static DefaultMutableTreeNode createRoot(RootIndexItem rootIndexItem) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileTreeNode("Muzyka", rootIndexItem));
        rootIndexItem.getChildren().stream()
                .filter(IndexItem::isDirectory)
                .map(FileTree::createNode)
                .forEach(root::add);
        return root;
    }

    private static DefaultMutableTreeNode createNode(IndexItem item) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new FileTreeNode(item.getName(), item));
        item.getChildren().stream()
                .filter(IndexItem::isDirectory)
                .map(FileTree::createNode)
                .forEach(node::add);
        return node;
    }

    private void notifySelectedFiles(TreePath path) {
        Object lastPathComponent = path.getLastPathComponent();
        if (!(lastPathComponent instanceof DefaultMutableTreeNode treeNode)
                || !(treeNode.getUserObject() instanceof FileTreeNode fileTreeNode)) {
            return;
        }

        selectedFilesConsumer.accept(collectFiles(fileTreeNode.indexItem()));
    }

    private List<IndexItem> collectFiles(IndexItem item) {
        List<IndexItem> files = new ArrayList<>();
        collectFiles(item, files);
        return files;
    }

    private void collectFiles(IndexItem item, List<IndexItem> files) {
        if (!item.isDirectory()) {
            files.add(item);
            return;
        }

        item.getChildren().forEach(child -> collectFiles(child, files));
    }

    private class FileTreeSelectionListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            TreePath path = getClosestPathForLocation(event.getX(), event.getY());
            if (path == null) {
                return;
            }

            Rectangle pathBounds = getPathBounds(path);
            if (pathBounds != null && pathBounds.contains(event.getPoint())) {
                setSelectionPath(path);
                notifySelectedFiles(path);
            }
        }
    }
}
