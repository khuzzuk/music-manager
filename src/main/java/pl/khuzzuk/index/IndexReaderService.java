package pl.khuzzuk.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class IndexReaderService {
    private final Path indexPath;

    public IndexReaderService(Path indexPath) {
        this.indexPath = indexPath;
    }

    public RootIndexItem read() throws IOException {
        List<String> lines = Files.readAllLines(indexPath, StandardCharsets.UTF_8);
        RootIndexItem root = null;
        Deque<IndexItem> directories = new ArrayDeque<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!directories.isEmpty()) {
                    directories.pop();
                }
                continue;
            }

            if (line.startsWith(IndexItem.DIRECTORY_PREFIX)) {
                IndexItem directory = createDirectory(unescape(line.substring(IndexItem.DIRECTORY_PREFIX.length())), root, directories);
                if (directory.isRoot()) {
                    root = (RootIndexItem) directory;
                }
                directories.push(directory);
                continue;
            }

            if (directories.isEmpty()) {
                throw new IOException("Sound file entry without directory: " + line);
            }

            SoundFileIndexItem item = new SoundFileIndexItem(unescape(line));
            item.setParent(directories.peek());
            directories.peek().getChildren().add(item);
        }

        return root == null ? new RootIndexItem() : root;
    }

    private IndexItem createDirectory(String name, RootIndexItem root, Deque<IndexItem> directories) throws IOException {
        if (root == null) {
            if (!IndexItem.ROOT_NAME.equals(name)) {
                throw new IOException("Index root must be " + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME);
            }
            return new RootIndexItem();
        }
        if (directories.isEmpty()) {
            throw new IOException("Directory entry without parent: " + name);
        }

        DirectoryIndexItem item = new DirectoryIndexItem(name);
        item.setParent(directories.peek());
        directories.peek().getChildren().add(item);
        return item;
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
