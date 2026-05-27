package pl.khuzzuk.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class IndexService {
    private final Path indexPath;

    public IndexService(Path indexPath) {
        this.indexPath = indexPath;
    }

    public RootIndexItem index(List<Path> rootPaths) throws IOException {
        RootIndexItem root = new RootIndexItem();
        List<IndexItem> children = rootPaths.stream()
                .filter(path -> rootPaths.stream().noneMatch(other -> isNestedPath(path, other)))
                .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                .map(path -> buildItem(path, root))
                .toList();
        root.addChildren(children);
        saveIndex(root);
        return root;
    }

    private IndexItem buildItem(Path path, IndexItem parent) {
        try {
            if (!Files.isDirectory(path)) {
                SoundFileIndexItem item = new SoundFileIndexItem(getName(path));
                item.setParent(parent);
                return item;
            }

            try (Stream<Path> children = Files.list(path)) {
                DirectoryIndexItem item = new DirectoryIndexItem(getName(path));
                item.setParent(parent);

                List<IndexItem> childItems = children
                        .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(child -> buildItem(child, item))
                        .toList();

                item.addChildren(childItems);
                return item;
            }
        } catch (IOException | SecurityException e) {
            UnreadableIndexItem item = new UnreadableIndexItem(getName(path));
            item.setParent(parent);
            return item;
        }
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
            index.append("D|");
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
