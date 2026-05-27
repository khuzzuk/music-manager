package pl.khuzzuk.index;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DirectoryIndexItem implements IndexItem {
    private final Path path;
    private final List<IndexItem> children = new ArrayList<>();
    private IndexItem parent;

    public DirectoryIndexItem(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public boolean isDirectory() {
        return true;
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
        return children;
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
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

    void addChildren(List<IndexItem> children) {
        this.children.addAll(children);
    }
}
