package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.index.RootIndexItem;
import pl.khuzzuk.logging.ErrorReporter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
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
    private static final String REINDEX_SELECTED_DIRECTORY_ACTION = "reindexSelectedDirectory";
    private static final String REINDEX_SELECTED_DIRECTORY_CHANGES_ACTION = "reindexSelectedDirectoryChanges";
    private final IndexService indexService;
    private final Consumer<List<IndexItem>> selectedFilesConsumer;
    private final FileTreeModeler modeler = new FileTreeModeler();

    public FileTree(Context context, Consumer<List<IndexItem>> selectedFilesConsumer) {
        super(createRoot(context.indexReaderService().getCurrentRootIndexItem()));
        this.indexService = context.indexService();
        this.selectedFilesConsumer = selectedFilesConsumer;
        setToggleClickCount(1);
        modeler.modelTree(this);
        updateUI();
        setCellRenderer(new FileTreeCellRenderer());
        addMouseListener(new FileTreeSelectionListener());
        registerReindexSelectedDirectoryAction();
        this.indexService.addIndexListener(root -> SwingUtilities.invokeLater(() -> refresh(root)));
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

        for (int selectedRow : selectedRows) {
            Rectangle rowBounds = getRowBounds(selectedRow);
            if (rowBounds != null) {
                modeler.paintSelectionRow(graphics, rowBounds, getWidth());
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
                reindexSelectedDirectory(false);
            }
        });
        getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.CTRL_DOWN_MASK),
                REINDEX_SELECTED_DIRECTORY_CHANGES_ACTION);
        getActionMap().put(REINDEX_SELECTED_DIRECTORY_CHANGES_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                reindexSelectedDirectory(true);
            }
        });
    }

    private void reindexSelectedDirectory(boolean changesOnly) {
        TreePath selectionPath = getSelectionPath();
        if (selectionPath == null) {
            return;
        }

        IndexItem selectedItem = getIndexItem(selectionPath);
        if (selectedItem == null || !selectedItem.isDirectory()) {
            return;
        }

        Action action = getActionMap().get(REINDEX_SELECTED_DIRECTORY_ACTION);
        Action changesAction = getActionMap().get(REINDEX_SELECTED_DIRECTORY_CHANGES_ACTION);
        action.setEnabled(false);
        changesAction.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                if (changesOnly) {
                    indexService.reindexDirectoryChanges(selectedItem);
                } else {
                    indexService.reindexDirectory(selectedItem);
                }
                return null;
            }

            @Override
            protected void done() {
                action.setEnabled(true);
                changesAction.setEnabled(true);
                try {
                    get();
                } catch (Exception e) {
                    ErrorReporter.log("Cannot reindex directory.", e);
                    JOptionPane.showMessageDialog(
                            FileTree.this,
                            "Nie udalo sie przeindeksowac katalogu.\n"
                                    + ErrorReporter.userMessage(e)
                                    + "\n\nStack trace zapisano w " + ErrorReporter.logFile(),
                            "Blad indeksowania",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }

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
