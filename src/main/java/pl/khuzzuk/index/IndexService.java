package pl.khuzzuk.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class IndexService {
    public RootIndexItem buildTree(List<Path> rootPaths) {
        RootIndexItem root = new RootIndexItem();
        List<IndexItem> children = rootPaths.stream()
                .filter(path -> rootPaths.stream().noneMatch(other -> isNestedPath(path, other)))
                .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                .map(path -> buildItem(path, root))
                .toList();
        root.addChildren(children);
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
}
