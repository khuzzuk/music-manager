package pl.khuzzuk.index;

import java.util.ArrayList;
import java.util.List;

public class RootIndexItem implements IndexItem {
    private final List<IndexItem> children = new ArrayList<>();

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public String getName() {
        return "root";
    }

    @Override
    public List<IndexItem> getChildren() {
        return children;
    }

    @Override
    public IndexItem getParent() {
        return null;
    }

    @Override
    public void setParent(IndexItem parent) {
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    void addChildren(List<IndexItem> children) {
        this.children.addAll(children);
    }
}
