package pl.khuzzuk.index;

import java.util.List;

public interface IndexItem {
    boolean isDirectory();

    String getName();

    List<IndexItem> getChildren();

    boolean hasChildren();

    IndexItem getParent();

    void setParent(IndexItem parent);

    boolean isRoot();
}
