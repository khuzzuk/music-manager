package pl.khuzzuk.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class IndexReaderService {
    private final Path indexPath;

    public IndexReaderService(Path indexPath) {
        this.indexPath = indexPath;
    }

    public RootIndexItem read() throws IOException {
        List<String> lines = Files.readAllLines(indexPath, StandardCharsets.UTF_8);
        RootIndexItem root = null;

        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith(IndexItem.DIRECTORY_PREFIX)) {
                IndexItem directory = createDirectory(unescape(line.substring(IndexItem.DIRECTORY_PREFIX.length())), root);
                if (directory.isRoot()) {
                    root = (RootIndexItem) directory;
                }
                continue;
            }

            if (root == null) {
                throw new IOException("Sound file entry without directory: " + line);
            }

            SoundFileIndexItem item = new SoundFileIndexItem(Path.of(unescape(line)));
            IndexItem parent = findParent(root, item.getPath());
            item.setParent(parent);
            parent.getChildren().add(item);
        }

        return root == null ? new RootIndexItem() : root;
    }

    private IndexItem createDirectory(String value, RootIndexItem root) throws IOException {
        if (root == null) {
            if (!IndexItem.ROOT_NAME.equals(value)) {
                throw new IOException("Index root must be " + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME);
            }
            return new RootIndexItem();
        }

        DirectoryIndexItem item = new DirectoryIndexItem(Path.of(value));
        IndexItem parent = findParent(root, item.getPath());
        item.setParent(parent);
        parent.getChildren().add(item);
        return item;
    }

    private IndexItem findParent(IndexItem root, Path path) {
        Path parentPath = path.getParent();
        if (parentPath == null) {
            return root;
        }
        return findDirectoryByPath(root, parentPath.toAbsolutePath().normalize());
    }

    private IndexItem findDirectoryByPath(IndexItem item, Path path) {
        if (path.equals(item.getPath())) {
            return item;
        }
        return item.getChildren().stream()
                .filter(IndexItem::isDirectory)
                .map(child -> findDirectoryByPath(child, path))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(item.isRoot() ? item : null);
    }

    private String unescape(String value) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (escaped) {
                result.append(unescape(character));
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                result.append(character);
            }
        }
        if (escaped) {
            throw new IOException("Invalid trailing escape in index entry: " + value);
        }
        return result.toString();
    }

    private char unescape(char character) {
        return switch (character) {
            case 'r' -> '\r';
            case 'n' -> '\n';
            default -> character;
        };
    }
}
