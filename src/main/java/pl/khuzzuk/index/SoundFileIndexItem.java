package pl.khuzzuk.index;

import java.util.List;

public class SoundFileIndexItem implements IndexItem {
    private final String name;
    private IndexItem parent;

    public SoundFileIndexItem(String name) {
        this.name = name;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getName() {
        return name;
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
