package pl.khuzzuk.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexServiceTest {
    private static final String SEPARATOR = System.lineSeparator();

    @TempDir
    private Path tempDir;

    @Test
    void writesRootAsDirectoryWithLeadingEmptyLine() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path indexPath = tempDir.resolve("index.dat");

        new IndexService(indexPath).index(List.of(music));

        assertEquals(
                SEPARATOR
                        + "D|root" + SEPARATOR
                        + "D|music" + SEPARATOR,
                Files.readString(indexPath));
    }

    @Test
    void writesDirectoriesWithPrefixAndFilesAsBareNames() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path album = Files.createDirectory(music.resolve("album"));
        Files.createFile(album.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        new IndexService(indexPath).index(List.of(music));

        assertEquals(
                SEPARATOR
                        + "D|root" + SEPARATOR
                        + "D|music" + SEPARATOR
                        + "D|album" + SEPARATOR
                        + "song.mp3" + SEPARATOR,
                Files.readString(indexPath));
    }

    @Test
    void sortsRootPathsAndChildrenCaseInsensitively() throws IOException {
        Path secondRoot = Files.createDirectory(tempDir.resolve("bravo"));
        Path firstRoot = Files.createDirectory(tempDir.resolve("Alpha"));
        Files.createFile(firstRoot.resolve("zulu.mp3"));
        Files.createFile(firstRoot.resolve("Echo.mp3"));
        Files.createFile(secondRoot.resolve("delta.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new IndexService(indexPath).index(List.of(secondRoot, firstRoot));

        assertEquals(List.of("Alpha", "bravo"), root.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(List.of("Echo.mp3", "zulu.mp3"), root.getChildren().getFirst().getChildren().stream()
                .map(IndexItem::getName)
                .toList());
    }

    @Test
    void filtersNestedRootPaths() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path album = Files.createDirectory(music.resolve("album"));
        Files.createFile(album.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new IndexService(indexPath).index(List.of(album, music));

        assertEquals(1, root.getChildren().size());
        assertEquals("music", root.getChildren().getFirst().getName());
        assertEquals("album", root.getChildren().getFirst().getChildren().getFirst().getName());
    }

    @Test
    void setsParentLinks() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new IndexService(indexPath).index(List.of(music));
        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();

        assertNull(root.getParent());
        assertSame(root, directory.getParent());
        assertSame(directory, file.getParent());
    }

    @Test
    void hasChildrenMatchesChildrenList() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new IndexService(indexPath).index(List.of(music));
        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();

        assertTrue(root.hasChildren());
        assertTrue(directory.hasChildren());
        assertFalse(file.hasChildren());
        assertInstanceOf(SoundFileIndexItem.class, file);
    }
}
