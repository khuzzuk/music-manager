package pl.khuzzuk.index;

import pl.khuzzuk.metadata.SoundFileMetadata;

import java.nio.file.Path;
import java.util.List;

public class SoundFileIndexItem implements IndexItem {
    private final Path path;
    private final SoundFileMetadata metadata;
    private IndexItem parent;

    public SoundFileIndexItem(Path path) {
        this(path, SoundFileMetadata.empty(path));
    }

    public SoundFileIndexItem(Path path, SoundFileMetadata metadata) {
        this.path = path.toAbsolutePath().normalize();
        this.metadata = metadata == null ? SoundFileMetadata.empty(path) : metadata;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getName() {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    public SoundFileMetadata getMetadata() {
        return metadata;
    }

    @Override
    public List<IndexItem> getChildren() {
        return List.of();
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public IndexItem getParent() {
        return parent;
    }

    @Override
    public void setParent(IndexItem parent) {
        this.parent = parent;
    }

    @Override
    public boolean isRoot() {
        return false;
    }
}
