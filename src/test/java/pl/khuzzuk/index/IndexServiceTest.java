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

        new IndexService(indexPath).index(new RootIndexItem(), List.of(music));

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

        new IndexService(indexPath).index(new RootIndexItem(), List.of(music));

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

        RootIndexItem root = new RootIndexItem();
        new IndexService(indexPath).index(root, List.of(secondRoot, firstRoot));

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

        RootIndexItem root = new RootIndexItem();
        new IndexService(indexPath).index(root, List.of(album, music));

        assertEquals(1, root.getChildren().size());
        assertEquals("music", root.getChildren().getFirst().getName());
        assertEquals("album", root.getChildren().getFirst().getChildren().getFirst().getName());
    }

    @Test
    void mergesMissingItemsIntoExistingRoot() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path albumPath = Files.createDirectory(music.resolve("album"));
        Files.createFile(albumPath.resolve("existing.mp3"));
        Files.createFile(albumPath.resolve("new.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem existingMusic = new DirectoryIndexItem("music");
        existingMusic.setParent(root);
        DirectoryIndexItem existingAlbum = new DirectoryIndexItem("album");
        existingAlbum.setParent(existingMusic);
        SoundFileIndexItem existingFile = new SoundFileIndexItem("existing.mp3");
        existingFile.setParent(existingAlbum);
        existingAlbum.addChildren(List.of(existingFile));
        existingMusic.addChildren(List.of(existingAlbum));
        root.addChildren(List.of(existingMusic));

        RootIndexItem indexedRoot = new IndexService(indexPath).index(root, List.of(music));

        assertSame(root, indexedRoot);
        assertEquals(1, root.getChildren().size());
        assertSame(existingMusic, root.getChildren().getFirst());
        assertSame(existingAlbum, root.getChildren().getFirst().getChildren().getFirst());
        assertEquals(List.of("existing.mp3", "new.mp3"), existingAlbum.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(existingFile, existingAlbum.getChildren().getFirst());
        assertSame(existingAlbum, existingAlbum.getChildren().getLast().getParent());
    }

    @Test
    void doesNotDuplicateExistingRootDirectoryWhenMerging() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem existingMusic = new DirectoryIndexItem("music");
        existingMusic.setParent(root);
        root.addChildren(List.of(existingMusic));

        new IndexService(indexPath).index(root, List.of(music));

        assertEquals(1, root.getChildren().size());
        assertSame(existingMusic, root.getChildren().getFirst());
        assertEquals(List.of("song.mp3"), existingMusic.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
    }

    @Test
    void mergesMissingNestedAlbumIntoExistingTree() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createDirectory(music.resolve("album1"));
        Path album2Path = Files.createDirectory(music.resolve("album2"));
        Files.createDirectory(album2Path.resolve("album3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem existingMusic = new DirectoryIndexItem("music");
        existingMusic.setParent(root);
        DirectoryIndexItem album1 = new DirectoryIndexItem("album1");
        album1.setParent(existingMusic);
        DirectoryIndexItem album2 = new DirectoryIndexItem("album2");
        album2.setParent(existingMusic);
        existingMusic.addChildren(List.of(album1, album2));
        root.addChildren(List.of(existingMusic));

        new IndexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("album1", "album2"), existingMusic.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(album1, existingMusic.getChildren().getFirst());
        assertSame(album2, existingMusic.getChildren().getLast());
        assertEquals(List.of("album3"), album2.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(album2, album2.getChildren().getFirst().getParent());
    }

    @Test
    void removesAlbumMissingFromDiskWhenMergingRoot() throws IOException {
        Path album1Path = Files.createDirectory(tempDir.resolve("album1"));
        Path album2Path = Files.createDirectory(tempDir.resolve("album2"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem album1 = new DirectoryIndexItem("album1");
        album1.setParent(root);
        DirectoryIndexItem album2 = new DirectoryIndexItem("album2");
        album2.setParent(root);
        DirectoryIndexItem album3 = new DirectoryIndexItem("album3");
        album3.setParent(root);
        root.addChildren(List.of(album1, album2, album3));

        new IndexService(indexPath).index(root, List.of(album1Path, album2Path));

        assertEquals(List.of("album1", "album2"), root.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(album1, root.getChildren().getFirst());
        assertSame(album2, root.getChildren().getLast());
        assertFalse(root.getChildren().contains(album3));
    }

    @Test
    void removesSoundFileMissingFromDiskWhenMergingDirectory() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path albumPath = Files.createDirectory(music.resolve("album"));
        Files.createFile(albumPath.resolve("song1.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem existingMusic = new DirectoryIndexItem("music");
        existingMusic.setParent(root);
        DirectoryIndexItem album = new DirectoryIndexItem("album");
        album.setParent(existingMusic);
        SoundFileIndexItem song1 = new SoundFileIndexItem("song1.mp3");
        song1.setParent(album);
        SoundFileIndexItem song2 = new SoundFileIndexItem("song2.mp3");
        song2.setParent(album);
        album.addChildren(List.of(song1, song2));
        existingMusic.addChildren(List.of(album));
        root.addChildren(List.of(existingMusic));

        new IndexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("song1.mp3"), album.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(song1, album.getChildren().getFirst());
        assertFalse(album.getChildren().contains(song2));
    }

    @Test
    void setsParentLinks() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        new IndexService(indexPath).index(root, List.of(music));
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

        RootIndexItem root = new RootIndexItem();
        new IndexService(indexPath).index(root, List.of(music));
        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();

        assertTrue(root.hasChildren());
        assertTrue(directory.hasChildren());
        assertFalse(file.hasChildren());
        assertInstanceOf(SoundFileIndexItem.class, file);
    }
}
