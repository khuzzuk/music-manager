package pl.khuzzuk.index;

import java.nio.file.Path;
import java.util.List;

public interface IndexItem {
    String DIRECTORY_PREFIX = "D|";
    String LINE_SEPARATOR = System.lineSeparator();
    String ROOT_NAME = "root";

    boolean isDirectory();

    String getName();

    Path getPath();

    List<IndexItem> getChildren();

    boolean hasChildren();

    IndexItem getParent();

    void setParent(IndexItem parent);

    boolean isRoot();
}
