package pl.khuzzuk.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class IndexReaderServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void readsIndexFileIntoTree() throws IOException {
        Path indexPath = tempDir.resolve("index.dat");
        Path musicPath = tempDir.resolve("music").toAbsolutePath().normalize();
        Path album1Path = musicPath.resolve("album1").toAbsolutePath().normalize();
        Path song1Path = album1Path.resolve("song1.mp3").toAbsolutePath().normalize();
        Path album2Path = musicPath.resolve("album2").toAbsolutePath().normalize();
        Path song2Path = album2Path.resolve("song2.mp3").toAbsolutePath().normalize();
        Files.writeString(indexPath,
                IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(musicPath.toString()) + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(album1Path.toString()) + IndexItem.LINE_SEPARATOR
                        + escape(song1Path.toString()) + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(album2Path.toString()) + IndexItem.LINE_SEPARATOR
                        + escape(song2Path.toString()) + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR);

        RootIndexItem root = new IndexReaderService(indexPath).read();

        assertEquals(IndexItem.ROOT_NAME, root.getName());
        assertNull(root.getParent());
        assertEquals(List.of("music"), root.getChildren().stream()
                .map(IndexItem::getName)
                .toList());

        IndexItem music = root.getChildren().getFirst();
        assertInstanceOf(DirectoryIndexItem.class, music);
        assertSame(root, music.getParent());
        assertEquals(musicPath, music.getPath());
        assertEquals(List.of("album1", "album2"), music.getChildren().stream()
                .map(IndexItem::getName)
                .toList());

        IndexItem album1 = music.getChildren().getFirst();
        IndexItem album2 = music.getChildren().getLast();
        assertEquals(List.of("song1.mp3"), album1.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(List.of("song2.mp3"), album2.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(album1Path, album1.getPath());
        assertEquals(album2Path, album2.getPath());
        assertEquals(song1Path, album1.getChildren().getFirst().getPath());
        assertEquals(song2Path, album2.getChildren().getFirst().getPath());
        assertSame(album1, album1.getChildren().getFirst().getParent());
        assertSame(album2, album2.getChildren().getFirst().getParent());
    }

    @Test
    void readsEscapedNames() throws IOException {
        Path indexPath = tempDir.resolve("index.dat");
        Path music = tempDir.resolve("music archive").toAbsolutePath().normalize();
        Path song = music.resolve("song one.mp3").toAbsolutePath().normalize();
        Files.writeString(indexPath,
                IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(music.toString()) + IndexItem.LINE_SEPARATOR
                        + escape(song.toString()) + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR
                        + IndexItem.LINE_SEPARATOR);

        RootIndexItem root = new IndexReaderService(indexPath).read();

        IndexItem musicItem = root.getChildren().getFirst();
        assertEquals("music archive", musicItem.getName());
        assertEquals(music, musicItem.getPath());
        assertEquals("song one.mp3", musicItem.getChildren().getFirst().getName());
        assertEquals(song, musicItem.getChildren().getFirst().getPath());
    }

    @Test
    void readsTreeWrittenByIndexService() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path album = Files.createDirectory(music.resolve("album"));
        Files.createFile(album.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        new IndexService(indexPath).index(new RootIndexItem(), List.of(music));
        RootIndexItem root = new IndexReaderService(indexPath).read();

        IndexItem indexedMusic = root.getChildren().getFirst();
        assertEquals("music", indexedMusic.getName());
        assertEquals(List.of("album"), indexedMusic.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(List.of("song.mp3"), indexedMusic.getChildren().getFirst().getChildren().stream()
                .map(IndexItem::getName)
                .toList());
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("|", "\\|");
    }
}
