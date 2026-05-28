package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.index.RootIndexItem;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileTree extends JTree {
    private static final Color SELECTION_BACKGROUND = new Color(218, 235, 252);
    private static final String REINDEX_SELECTED_DIRECTORY_ACTION = "reindexSelectedDirectory";
    private final IndexService indexService;
    private final Consumer<List<IndexItem>> selectedFilesConsumer;

    public FileTree(
            IndexReaderService indexReaderService,
            IndexService indexService,
            Consumer<List<IndexItem>> selectedFilesConsumer) {
        super(createRoot(indexReaderService.getCurrentRootIndexItem()));
        this.indexService = indexService;
        this.selectedFilesConsumer = selectedFilesConsumer;
        setOpaque(false);
        setToggleClickCount(1);
        putClientProperty("JTree.lineStyle", "None");
        UIManager.put("Tree.collapsedIcon", new TreeToggleIcon(false));
        UIManager.put("Tree.expandedIcon", new TreeToggleIcon(true));
        updateUI();
        setCellRenderer(new FileTreeCellRenderer());
        addMouseListener(new FileTreeSelectionListener());
        registerReindexSelectedDirectoryAction();
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
        IndexItem selectedItem = getSelectedIndexItem();
        DefaultMutableTreeNode root = createRoot(rootIndexItem);
        setModel(new DefaultTreeModel(root));
        expandRow(0);
        restoreSelection(root, selectedItem);
    }

    private void registerReindexSelectedDirectoryAction() {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), REINDEX_SELECTED_DIRECTORY_ACTION);
        getActionMap().put(REINDEX_SELECTED_DIRECTORY_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                reindexSelectedDirectory();
            }
        });
    }

    private void reindexSelectedDirectory() {
        TreePath selectionPath = getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        IndexItem selectedItem = getIndexItem(selectionPath);
        if (selectedItem == null || !selectedItem.isDirectory()) {
            return;
        }

        Action action = getActionMap().get(REINDEX_SELECTED_DIRECTORY_ACTION);
        action.setEnabled(false);
        try {
            indexService.reindexDirectory(selectedItem);
            selectedFilesConsumer.accept(collectFiles(selectedItem));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nie udalo sie przeindeksowac katalogu.",
                    "Blad indeksowania",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            action.setEnabled(true);
        }
    }

    private IndexItem getSelectedIndexItem() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath == null ? null : getIndexItem(selectionPath);
    }

    private IndexItem getIndexItem(TreePath path) {
        Object lastPathComponent = path.getLastPathComponent();
        if (!(lastPathComponent instanceof DefaultMutableTreeNode treeNode)
                || !(treeNode.getUserObject() instanceof FileTreeNode fileTreeNode)) {
            return null;
        }

        return fileTreeNode.indexItem();
    }

    private void restoreSelection(DefaultMutableTreeNode root, IndexItem selectedItem) {
        if (selectedItem == null) {
            return;
        }

        TreePath selectionPath = findPath(root, selectedItem);
        if (selectionPath != null) {
            setSelectionPath(selectionPath);
        }
    }

    private TreePath findPath(DefaultMutableTreeNode node, IndexItem selectedItem) {
        if (node.getUserObject() instanceof FileTreeNode fileTreeNode
                && isSameIndexItem(fileTreeNode.indexItem(), selectedItem)) {
            return new TreePath(node.getPath());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            TreePath path = findPath((DefaultMutableTreeNode) node.getChildAt(i), selectedItem);
            if (path != null) {
                return path;
            }
        }

        return null;
    }

    private boolean isSameIndexItem(IndexItem item, IndexItem selectedItem) {
        if (item.isRoot() || selectedItem.isRoot()) {
            return item.isRoot() && selectedItem.isRoot();
        }

        return item.getPath() != null && item.getPath().equals(selectedItem.getPath());
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
        IndexItem selectedItem = getIndexItem(path);
        if (selectedItem == null) {
            return;
        }

        selectedFilesConsumer.accept(collectFiles(selectedItem));
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
