package pl.khuzzuk.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexService {
    private final Path indexPath;

    public IndexService(Path indexPath) {
        this.indexPath = indexPath;
    }

    /**
     * Extends the supplied root item with entries found under {@code rootPaths}
     * and writes the merged tree to the configured index file.
     * <p>
     * Hard assumption: every path in {@code rootPaths} is a directory.
     */
    public RootIndexItem index(RootIndexItem root, List<Path> rootPaths) throws IOException {
        List<Path> children = rootPaths.stream()
                .filter(path -> rootPaths.stream().noneMatch(other -> isNestedPath(path, other)))
                .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        removeMissingChildren(root, children);
        children.forEach(path -> mergeDirectory(root, path));
        sortChildren(root);
        saveIndex(root);
        return root;
    }

    private void mergeDirectory(IndexItem parent, Path path) {
        DirectoryIndexItem directory = findDirectory(parent, getName(path));
        boolean createdDirectory = directory == null;
        if (directory == null) {
            directory = new DirectoryIndexItem(getName(path));
            directory.setParent(parent);
            parent.getChildren().add(directory);
        }

        try {
            try (Stream<Path> children = Files.list(path)) {
                DirectoryIndexItem targetDirectory = directory;
                List<Path> childPaths = children
                        .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList();
                removeMissingChildren(targetDirectory, childPaths);
                childPaths.forEach(child -> mergeChild(targetDirectory, child));
                sortChildren(directory);
            }
        } catch (IOException | SecurityException e) {
            if (createdDirectory) {
                parent.getChildren().remove(directory);
                UnreadableIndexItem item = new UnreadableIndexItem(getName(path));
                item.setParent(parent);
                parent.getChildren().add(item);
            }
        }
    }

    private void removeMissingChildren(IndexItem parent, List<Path> childPaths) {
        Set<String> existingNames = childPaths.stream()
                .map(this::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        parent.getChildren().removeIf(child -> !existingNames.contains(child.getName().toLowerCase()));
    }

    private void mergeChild(DirectoryIndexItem parent, Path path) {
        try {
            if (Files.isDirectory(path)) {
                mergeDirectory(parent, path);
                return;
            }

            if (findChild(parent, getName(path), SoundFileIndexItem.class) == null) {
                SoundFileIndexItem item = new SoundFileIndexItem(getName(path));
                item.setParent(parent);
                parent.getChildren().add(item);
            }
        } catch (SecurityException e) {
            UnreadableIndexItem item = new UnreadableIndexItem(getName(path));
            item.setParent(parent);
            parent.getChildren().add(item);
        }
    }

    private DirectoryIndexItem findDirectory(IndexItem parent, String name) {
        return findChild(parent, name, DirectoryIndexItem.class);
    }

    private <T extends IndexItem> T findChild(IndexItem parent, String name, Class<T> type) {
        return parent.getChildren().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .filter(child -> child.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void sortChildren(IndexItem item) {
        List<IndexItem> sortedChildren = new ArrayList<>(item.getChildren());
        sortedChildren.sort(Comparator.comparing(IndexItem::getName, String.CASE_INSENSITIVE_ORDER));
        item.getChildren().clear();
        item.getChildren().addAll(sortedChildren);
    }

    private boolean isNestedPath(Path path, Path possibleParent) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        Path normalizedParent = possibleParent.toAbsolutePath().normalize();
        return !normalizedPath.equals(normalizedParent)
                && normalizedPath.startsWith(normalizedParent);
    }

    private String getName(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    private void saveIndex(IndexItem root) throws IOException {
        StringBuilder index = new StringBuilder();
        appendIndexItem(index, root);
        Files.writeString(indexPath, index.toString(), StandardCharsets.UTF_8);
    }

    private void appendIndexItem(StringBuilder index, IndexItem item) {
        if (item instanceof UnreadableIndexItem) {
            return;
        }

        if (item.isRoot()) {
            index.append(System.lineSeparator());
        }

        if (item.isRoot() || item instanceof DirectoryIndexItem) {
            index.append(IndexItem.DIRECTORY_PREFIX);
        }

        index.append(escape(item.getName())).append(System.lineSeparator());
        if (!item.hasChildren()) {
            return;
        }

        item.getChildren().forEach(child -> appendIndexItem(index, child));
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("|", "\\|");
    }
}
