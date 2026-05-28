package pl.khuzzuk.index;

import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexService {
    private final Path indexPath;
    private final MetadataReaderService metadataReaderService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final List<Consumer<RootIndexItem>> indexListeners = new ArrayList<>();

    public IndexService(
            Path indexPath,
            MetadataReaderService metadataReaderService,
            MetadataIndexWriterService metadataIndexWriterService) {
        this.indexPath = indexPath;
        this.metadataReaderService = metadataReaderService;
        this.metadataIndexWriterService = metadataIndexWriterService;
    }

    public void addIndexListener(Consumer<RootIndexItem> listener) {
        indexListeners.add(listener);
    }

    /**
     * Extends the supplied root item with entries found under {@code rootPaths}
     * and writes the merged tree to the configured index file.
     * <p>
     * Hard assumption: every path in {@code rootPaths} is a directory.
     */
    public RootIndexItem index(RootIndexItem root, List<Path> rootPaths) throws IOException {
        List<Path> normalizedRootPaths = rootPaths.stream()
                .map(p -> p.toAbsolutePath().normalize())
                .distinct()
                .toList();
        List<Path> children = normalizedRootPaths.stream()
                .filter(path -> normalizedRootPaths.stream().noneMatch(other -> !path.equals(other) && path.startsWith(other)))
                .sorted(Comparator.comparing(this::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        removeMissingChildren(root, children);
        children.forEach(path -> mergeDirectory(root, path));
        sortChildren(root);
        saveIndex(root);
        notifyIndexListeners(root);
        return root;
    }

    public void reindexDirectory(IndexItem directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Only directories can be reindexed.");
        }

        RootIndexItem root = findRoot(directory);
        if (directory.isRoot()) {
            List<Path> rootPaths = root.getChildren().stream()
                    .filter(IndexItem::isDirectory)
                    .map(IndexItem::getPath)
                    .toList();
            index(root, rootPaths);
            return;
        }

        IndexItem parent = directory.getParent();
        if (parent == null || directory.getPath() == null) {
            throw new IllegalArgumentException("Directory must belong to an index tree.");
        }

        mergeDirectory(parent, directory.getPath());
        sortChildren(parent);
        saveIndex(root);
        notifyIndexListeners(root);
    }

    private RootIndexItem findRoot(IndexItem item) {
        IndexItem current = item;
        while (current.getParent() != null) {
            current = current.getParent();
        }

        if (current instanceof RootIndexItem root) {
            return root;
        }

        throw new IllegalArgumentException("Directory must belong to an index tree.");
    }

    private void notifyIndexListeners(RootIndexItem root) {
        indexListeners.forEach(listener -> listener.accept(root));
    }

    private void mergeDirectory(IndexItem parent, Path path) {
        DirectoryIndexItem directory = findDirectory(parent, getName(path));
        boolean createdDirectory = directory == null;
        if (directory == null) {
            directory = new DirectoryIndexItem(path);
            directory.setParent(parent);
            parent.getChildren().add(directory);
        }

        try {
            try (Stream<Path> children = Files.list(path)) {
                DirectoryIndexItem targetDirectory = directory;
                List<Path> childPaths = children
                        .map(path1 -> path1.toAbsolutePath().normalize())
                        .filter(this::isIndexableChild)
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

            writeMetadata(path);
            if (findChild(parent, getName(path), SoundFileIndexItem.class) == null) {
                SoundFileIndexItem item = new SoundFileIndexItem(path);
                item.setParent(parent);
                parent.getChildren().add(item);
            }
        } catch (SecurityException e) {
            UnreadableIndexItem item = new UnreadableIndexItem(getName(path));
            item.setParent(parent);
            parent.getChildren().add(item);
        }
    }

    private void writeMetadata(Path path) {
        try {
            SoundFileMetadata metadata = metadataReaderService.readMetadata(path);
            metadataIndexWriterService.writeMetadata(metadata);
        } catch (IOException | SecurityException e) {
            // Metadata indexing is best-effort; filesystem indexing should continue.
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

    private boolean isIndexableChild(Path path) {
        try {
            return Files.isDirectory(path) || isSupportedSoundFile(path);
        } catch (SecurityException e) {
            return true;
        }
    }

    private boolean isSupportedSoundFile(Path path) {
        return SoundFileType.fromPath(path).isPresent();
    }

    private void sortChildren(IndexItem item) {
        List<IndexItem> sortedChildren = new ArrayList<>(item.getChildren());
        sortedChildren.sort(Comparator.comparing(IndexItem::getName, String.CASE_INSENSITIVE_ORDER));
        item.getChildren().clear();
        item.getChildren().addAll(sortedChildren);
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

        index.append(escape(getPersistentValue(item))).append(System.lineSeparator());
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

    private String getPersistentValue(IndexItem item) {
        return item.isRoot() ? item.getName() : item.getPath().toString();
    }
}
