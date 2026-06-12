package pl.khuzzuk.index;

import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndexService {
    private final Path indexPath;
    private final MetadataReaderService metadataReaderService;
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final List<Consumer<RootIndexItem>> indexListeners = new ArrayList<>();
    private final List<Consumer<IndexProgress>> progressListeners = new ArrayList<>();

    public IndexService(
            Path indexPath,
            MetadataReaderService metadataReaderService,
            MetadataIndexWriterService metadataIndexWriterService) {
        this(indexPath, metadataReaderService, null, metadataIndexWriterService);
    }

    public IndexService(
            Path indexPath,
            MetadataReaderService metadataReaderService,
            MetadataIndexReaderService metadataIndexReaderService,
            MetadataIndexWriterService metadataIndexWriterService) {
        this.indexPath = indexPath;
        this.metadataReaderService = metadataReaderService;
        this.metadataIndexReaderService = metadataIndexReaderService;
        this.metadataIndexWriterService = metadataIndexWriterService;
    }

    public void addIndexListener(Consumer<RootIndexItem> listener) {
        indexListeners.add(listener);
    }

    public void addProgressListener(Consumer<IndexProgress> listener) {
        progressListeners.add(listener);
    }

    /**
     * Extends the supplied root item with entries found under {@code rootPaths}
     * and writes the merged tree to the configured index file.
     * <p>
     * Hard assumption: every path in {@code rootPaths} is a directory.
     */
    public RootIndexItem index(RootIndexItem root, List<Path> rootPaths) throws IOException {
        notifyProgressListeners(IndexProgress.started());
        try {
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
            indexMetadata(root);
            notifyIndexListeners(root);
            return root;
        } finally {
            notifyProgressListeners(IndexProgress.finished());
        }
    }

    public void reindexDirectory(IndexItem directory) throws IOException {
        reindexDirectory(directory, false);
    }

    public void reindexDirectoryChanges(IndexItem directory) throws IOException {
        reindexDirectory(directory, true);
    }

    public void removeIndexedFiles(RootIndexItem root, Collection<Path> paths) throws IOException {
        if (root == null || paths == null || paths.isEmpty()) {
            return;
        }

        Set<Path> normalizedPaths = paths.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .collect(Collectors.toSet());
        if (!removeIndexedFilesFromTree(root, normalizedPaths)) {
            return;
        }

        saveIndex(root);
        notifyIndexListeners(root);
    }

    private void reindexDirectory(IndexItem directory, boolean changesOnly) throws IOException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Only directories can be reindexed.");
        }

        RootIndexItem root = findRoot(directory);
        if (directory.isRoot()) {
            List<Path> rootPaths = root.getChildren().stream()
                    .filter(IndexItem::isDirectory)
                    .map(IndexItem::getPath)
                    .toList();
            if (changesOnly) {
                reindexRootChanges(root, rootPaths);
            } else {
                index(root, rootPaths);
            }
            return;
        }

        IndexItem parent = directory.getParent();
        if (parent == null || directory.getPath() == null) {
            throw new IllegalArgumentException("Directory must belong to an index tree.");
        }

        notifyProgressListeners(IndexProgress.started());
        try {
            List<Path> previousMetadataPaths = collectSoundFilePaths(directory);
            mergeDirectory(parent, directory.getPath());
            sortChildren(parent);
            saveIndex(root);
            if (changesOnly) {
                indexChangedMetadata(previousMetadataPaths, collectSoundFilePaths(directory));
            } else {
                indexMetadata(directory);
            }
            notifyIndexListeners(root);
        } finally {
            notifyProgressListeners(IndexProgress.finished());
        }
    }

    private void reindexRootChanges(RootIndexItem root, List<Path> rootPaths) throws IOException {
        notifyProgressListeners(IndexProgress.started());
        try {
            List<Path> previousMetadataPaths = collectSoundFilePaths(root);
            removeMissingChildren(root, rootPaths);
            rootPaths.forEach(path -> mergeDirectory(root, path));
            sortChildren(root);
            saveIndex(root);
            indexChangedMetadata(previousMetadataPaths, collectSoundFilePaths(root));
            notifyIndexListeners(root);
        } finally {
            notifyProgressListeners(IndexProgress.finished());
        }
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

    private void notifyProgressListeners(IndexProgress progress) {
        progressListeners.forEach(listener -> listener.accept(progress));
    }

    private boolean removeIndexedFilesFromTree(IndexItem parent, Set<Path> paths) {
        boolean removed = parent.getChildren().removeIf(child ->
                child instanceof SoundFileIndexItem
                        && child.getPath() != null
                        && paths.contains(child.getPath().toAbsolutePath().normalize()));
        for (IndexItem child : parent.getChildren()) {
            if (child.hasChildren()) {
                removed |= removeIndexedFilesFromTree(child, paths);
            }
        }
        return removed;
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

    private void indexMetadata(IndexItem item) {
        List<Path> metadataPaths = collectSoundFilePaths(item);
        notifyProgressListeners(IndexProgress.metadata(0, metadataPaths.size()));
        List<SoundFileMetadata> metadataItems = new ArrayList<>();
        for (int i = 0; i < metadataPaths.size(); i++) {
            readMetadata(metadataPaths.get(i), metadataItems);
            notifyProgressListeners(IndexProgress.metadata(i + 1, metadataPaths.size()));
        }
        writeMetadata(metadataItems);
    }

    private void indexChangedMetadata(List<Path> previousMetadataPaths, List<Path> currentMetadataPaths) {
        List<Path> deletedMetadataPaths = difference(previousMetadataPaths, currentMetadataPaths);
        deleteMetadata(deletedMetadataPaths);
        List<Path> changedMetadataPaths = findChangedMetadataPaths(currentMetadataPaths);

        notifyProgressListeners(IndexProgress.metadata(0, changedMetadataPaths.size()));
        List<SoundFileMetadata> metadataItems = new ArrayList<>();
        for (int i = 0; i < changedMetadataPaths.size(); i++) {
            readMetadata(changedMetadataPaths.get(i), metadataItems);
            notifyProgressListeners(IndexProgress.metadata(i + 1, changedMetadataPaths.size()));
        }
        writeMetadata(metadataItems);
    }

    private List<Path> collectSoundFilePaths(IndexItem item) {
        List<Path> metadataPaths = new ArrayList<>();
        collectSoundFilePaths(item, metadataPaths);
        return metadataPaths;
    }

    private void collectSoundFilePaths(IndexItem item, List<Path> metadataPaths) {
        if (item instanceof SoundFileIndexItem && item.getPath() != null) {
            metadataPaths.add(item.getPath());
            return;
        }

        if (!item.hasChildren()) {
            return;
        }

        item.getChildren().forEach(child -> collectSoundFilePaths(child, metadataPaths));
    }

    private void readMetadata(Path path, List<SoundFileMetadata> metadataItems) {
        try {
            metadataItems.add(metadataReaderService.readMetadata(path));
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot read metadata while indexing: " + path, e);
        }
    }

    private void writeMetadata(List<SoundFileMetadata> metadataItems) {
        try {
            metadataIndexWriterService.writeMetadata(metadataItems);
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot write metadata index.", e);
        }
    }

    private void deleteMetadata(List<Path> metadataPaths) {
        try {
            metadataIndexWriterService.deleteMetadata(metadataPaths);
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot delete metadata from index.", e);
        }
    }

    private List<Path> findChangedMetadataPaths(List<Path> metadataPaths) {
        if (metadataIndexReaderService == null) {
            return metadataPaths;
        }

        try {
            Set<Path> currentCachedPaths = metadataIndexReaderService.readMetadata(metadataPaths).keySet();
            return metadataPaths.stream()
                    .filter(path -> !currentCachedPaths.contains(path.toAbsolutePath().normalize()))
                    .toList();
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot read metadata index while detecting changed files.", e);
            return metadataPaths;
        }
    }

    private List<Path> difference(List<Path> previousPaths, List<Path> currentPaths) {
        Set<Path> currentPathSet = new HashSet<>(currentPaths);
        return previousPaths.stream()
                .filter(path -> !currentPathSet.contains(path))
                .toList();
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
