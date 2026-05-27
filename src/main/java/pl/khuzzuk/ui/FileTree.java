package pl.khuzzuk.ui;

import pl.khuzzuk.settings.SettingsService;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.nio.file.Path;

public class FileTree extends JTree {
    public FileTree(SettingsService settingsService) {
        super(createRoot(settingsService));
    }

    private static DefaultMutableTreeNode createRoot(SettingsService settingsService) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Muzyka");
        settingsService.getSettings().indexedPaths().stream()
                .map(FileTree::getDirectoryName)
                .map(DefaultMutableTreeNode::new)
                .forEach(root::add);
        return root;
    }

    private static String getDirectoryName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
