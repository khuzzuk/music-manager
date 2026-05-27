package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;

record FileTreeNode(String displayName, IndexItem indexItem) {
    @Override
    public String toString() {
        return displayName;
    }
}
