package pl.khuzzuk.index;

import java.util.ArrayList;
import java.util.List;

public class DirectoryIndexItem implements IndexItem {
    private final String name;
    private final List<IndexItem> children = new ArrayList<>();
    private IndexItem parent;

    public DirectoryIndexItem(String name) {
        this.name = name;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IndexItem> getChildren() {
        return children;
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
