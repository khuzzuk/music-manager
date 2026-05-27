package pl.khuzzuk.index;

import java.nio.file.Path;
import java.util.List;

public class SoundFileIndexItem implements IndexItem {
    private final Path path;
    private IndexItem parent;

    public SoundFileIndexItem(Path path) {
        this.path = path.toAbsolutePath().normalize();
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
