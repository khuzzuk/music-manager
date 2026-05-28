package pl.khuzzuk.index;

import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.DocumentMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void writesRootAsDirectoryWithLeadingEmptyLine() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path indexPath = tempDir.resolve("index.dat");

        indexService(indexPath).index(new RootIndexItem(), List.of(music));

        assertEquals(
                IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(music.toAbsolutePath().normalize().toString()) + IndexItem.LINE_SEPARATOR,
                Files.readString(indexPath));
    }

    @Test
    void writesDirectoriesWithPrefixAndFilesAsBareNames() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path album = Files.createDirectory(music.resolve("album"));
        Files.createFile(album.resolve("song.mp3"));
        Files.createFile(album.resolve("notes.txt"));
        Path indexPath = tempDir.resolve("index.dat");

        indexService(indexPath).index(new RootIndexItem(), List.of(music));

        assertEquals(
                IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + IndexItem.ROOT_NAME + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(music.toAbsolutePath().normalize().toString()) + IndexItem.LINE_SEPARATOR
                        + IndexItem.DIRECTORY_PREFIX + escape(album.toAbsolutePath().normalize().toString()) + IndexItem.LINE_SEPARATOR
                        + escape(album.resolve("song.mp3").toAbsolutePath().normalize().toString()) + IndexItem.LINE_SEPARATOR,
                Files.readString(indexPath));
    }

    @Test
    void indexesOnlySupportedSoundFileExtensions() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Files.createFile(music.resolve("album.flac"));
        Files.createFile(music.resolve("notes.txt"));
        Files.createFile(music.resolve("cover.jpg"));
        Files.createFile(music.resolve("README"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        indexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("album.flac", "song.mp3"), root.getChildren().getFirst().getChildren().stream()
                .map(IndexItem::getName)
                .toList());
    }

    @Test
    void matchesSupportedSoundFileExtensionsCaseInsensitively() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.MP3"));
        Files.createFile(music.resolve("album.FLAC"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        indexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("album.FLAC", "song.MP3"), root.getChildren().getFirst().getChildren().stream()
                .map(IndexItem::getName)
                .toList());
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
        indexService(indexPath).index(root, List.of(secondRoot, firstRoot));

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
        indexService(indexPath).index(root, List.of(album, music));

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
        DirectoryIndexItem existingMusic = new DirectoryIndexItem(music);
        existingMusic.setParent(root);
        DirectoryIndexItem existingAlbum = new DirectoryIndexItem(albumPath);
        existingAlbum.setParent(existingMusic);
        SoundFileIndexItem existingFile = new SoundFileIndexItem(albumPath.resolve("existing.mp3"));
        existingFile.setParent(existingAlbum);
        existingAlbum.addChildren(List.of(existingFile));
        existingMusic.addChildren(List.of(existingAlbum));
        root.addChildren(List.of(existingMusic));

        RootIndexItem indexedRoot = indexService(indexPath).index(root, List.of(music));

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
        DirectoryIndexItem existingMusic = new DirectoryIndexItem(music);
        existingMusic.setParent(root);
        root.addChildren(List.of(existingMusic));

        indexService(indexPath).index(root, List.of(music));

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
        DirectoryIndexItem existingMusic = new DirectoryIndexItem(music);
        existingMusic.setParent(root);
        DirectoryIndexItem album1 = new DirectoryIndexItem(music.resolve("album1"));
        album1.setParent(existingMusic);
        DirectoryIndexItem album2 = new DirectoryIndexItem(album2Path);
        album2.setParent(existingMusic);
        existingMusic.addChildren(List.of(album1, album2));
        root.addChildren(List.of(existingMusic));

        indexService(indexPath).index(root, List.of(music));

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
        DirectoryIndexItem album1 = new DirectoryIndexItem(album1Path);
        album1.setParent(root);
        DirectoryIndexItem album2 = new DirectoryIndexItem(album2Path);
        album2.setParent(root);
        DirectoryIndexItem album3 = new DirectoryIndexItem(tempDir.resolve("album3"));
        album3.setParent(root);
        root.addChildren(List.of(album1, album2, album3));

        indexService(indexPath).index(root, List.of(album1Path, album2Path));

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
        DirectoryIndexItem existingMusic = new DirectoryIndexItem(music);
        existingMusic.setParent(root);
        DirectoryIndexItem album = new DirectoryIndexItem(albumPath);
        album.setParent(existingMusic);
        SoundFileIndexItem song1 = new SoundFileIndexItem(albumPath.resolve("song1.mp3"));
        song1.setParent(album);
        SoundFileIndexItem song2 = new SoundFileIndexItem(albumPath.resolve("song2.mp3"));
        song2.setParent(album);
        album.addChildren(List.of(song1, song2));
        existingMusic.addChildren(List.of(album));
        root.addChildren(List.of(existingMusic));

        indexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("song1.mp3"), album.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(song1, album.getChildren().getFirst());
        assertFalse(album.getChildren().contains(song2));
    }

    @Test
    void removesUnsupportedSoundFileWhenMergingDirectory() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path albumPath = Files.createDirectory(music.resolve("album"));
        Files.createFile(albumPath.resolve("song.mp3"));
        Files.createFile(albumPath.resolve("notes.txt"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        DirectoryIndexItem existingMusic = new DirectoryIndexItem(music);
        existingMusic.setParent(root);
        DirectoryIndexItem album = new DirectoryIndexItem(albumPath);
        album.setParent(existingMusic);
        SoundFileIndexItem song = new SoundFileIndexItem(albumPath.resolve("song.mp3"));
        song.setParent(album);
        SoundFileIndexItem notes = new SoundFileIndexItem(albumPath.resolve("notes.txt"));
        notes.setParent(album);
        album.addChildren(List.of(song, notes));
        existingMusic.addChildren(List.of(album));
        root.addChildren(List.of(existingMusic));

        indexService(indexPath).index(root, List.of(music));

        assertEquals(List.of("song.mp3"), album.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertSame(song, album.getChildren().getFirst());
        assertFalse(album.getChildren().contains(notes));
    }

    @Test
    void reindexesDirectoryWithoutRemovingSiblingRootDirectories() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path album = Files.createDirectory(music.resolve("album"));
        Path other = Files.createDirectory(tempDir.resolve("other"));
        Path oldSong = Files.createFile(album.resolve("old.mp3"));
        Path otherSong = Files.createFile(other.resolve("other.mp3"));
        Path indexPath = tempDir.resolve("index.dat");
        RootIndexItem root = new RootIndexItem();
        IndexService indexService = indexService(indexPath);
        indexService.index(root, List.of(music, other));
        IndexItem musicItem = root.getChildren().stream()
                .filter(item -> item.getName().equals("music"))
                .findFirst()
                .orElseThrow();
        IndexItem albumItem = musicItem.getChildren().getFirst();

        Files.delete(oldSong);
        Path newSong = Files.createFile(album.resolve("new.mp3"));
        indexService.reindexDirectory(albumItem);

        assertEquals(List.of("music", "other"), root.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(List.of("new.mp3"), albumItem.getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        assertEquals(List.of("other.mp3"), root.getChildren().stream()
                .filter(item -> item.getName().equals("other"))
                .findFirst()
                .orElseThrow()
                .getChildren().stream()
                .map(IndexItem::getName)
                .toList());
        String persistedIndex = Files.readString(indexPath);
        assertFalse(persistedIndex.contains(escape(oldSong.toAbsolutePath().normalize().toString())));
        assertTrue(persistedIndex.contains(escape(newSong.toAbsolutePath().normalize().toString())));
        assertTrue(persistedIndex.contains(escape(otherSong.toAbsolutePath().normalize().toString())));
    }

    @Test
    void setsParentLinks() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        indexService(indexPath).index(root, List.of(music));
        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();

        assertNull(root.getParent());
        assertSame(root, directory.getParent());
        assertSame(directory, file.getParent());
    }

    @Test
    void storesAbsoluteNormalizedPaths() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path song = Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");
        Path relativeMusic = tempDir.relativize(music);

        RootIndexItem root = new RootIndexItem();
        indexService(indexPath).index(root, List.of(tempDir.resolve(relativeMusic).resolve(".")));

        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();
        assertEquals(music.toAbsolutePath().normalize(), directory.getPath());
        assertEquals(song.toAbsolutePath().normalize(), file.getPath());
    }

    @Test
    void unreadablePathReturnsParentPath() {
        Path musicPath = tempDir.resolve("music").toAbsolutePath().normalize();
        DirectoryIndexItem music = new DirectoryIndexItem(musicPath);
        UnreadableIndexItem unreadable = new UnreadableIndexItem("broken");
        unreadable.setParent(music);

        assertEquals(musicPath, unreadable.getPath());
    }

    @Test
    void hasChildrenMatchesChildrenList() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");

        RootIndexItem root = new RootIndexItem();
        indexService(indexPath).index(root, List.of(music));
        IndexItem directory = root.getChildren().getFirst();
        IndexItem file = directory.getChildren().getFirst();

        assertTrue(root.hasChildren());
        assertTrue(directory.hasChildren());
        assertFalse(file.hasChildren());
        assertInstanceOf(SoundFileIndexItem.class, file);
    }

    @Test
    void writesMetadataWhenIndexingSoundFiles() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path song = Files.createFile(music.resolve("song.mp3"));
        Path indexPath = tempDir.resolve("index.dat");
        SoundFileMetadata metadata = SoundFileMetadata.empty(song);
        AtomicReference<SoundFileMetadata> writtenMetadata = new AtomicReference<>();
        MetadataReaderService metadataReaderService = new MetadataReaderService() {
            @Override
            public SoundFileMetadata readMetadata(Path path) {
                assertEquals(song.toAbsolutePath().normalize(), path);
                return metadata;
            }
        };
        MetadataWriterService metadataWriterService = new MetadataWriterService(
                tempDir.resolve("metadata-index"),
                new DocumentMapper()) {
            @Override
            public void writeMetadata(SoundFileMetadata metadata) {
                writtenMetadata.set(metadata);
            }
        };

        new IndexService(indexPath, metadataReaderService, metadataWriterService).index(new RootIndexItem(), List.of(music));

        assertSame(metadata, writtenMetadata.get());
    }

    @Test
    void notifiesListenersAfterSuccessfulIndex() throws IOException {
        Path music = Files.createDirectory(tempDir.resolve("music"));
        Path indexPath = tempDir.resolve("index.dat");
        RootIndexItem root = new RootIndexItem();
        IndexService indexService = indexService(indexPath);
        AtomicReference<RootIndexItem> notifiedRoot = new AtomicReference<>();
        indexService.addIndexListener(notifiedRoot::set);

        indexService.index(root, List.of(music));

        assertSame(root, notifiedRoot.get());
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("|", "\\|");
    }

    private IndexService indexService(Path indexPath) {
        return new IndexService(
                indexPath,
                new MetadataReaderService(),
                new MetadataWriterService(tempDir.resolve("metadata-index"), new DocumentMapper()));
    }
}
