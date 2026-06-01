package pl.khuzzuk.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MetadataIndexReaderServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void readsCurrentMetadataFromLuceneIndex() throws IOException {
        Path indexPath = tempDir.resolve("metadata-index");
        Path song = Files.createFile(tempDir.resolve("song.mp3"));
        DocumentMapper documentMapper = new DocumentMapper();
        MetadataIndexWriterService writerService = new MetadataIndexWriterService(indexPath, documentMapper);
        MetadataIndexReaderService readerService = new MetadataIndexReaderService(indexPath, documentMapper);
        SoundFileMetadata metadata = SoundFileMetadata.empty(song);
        metadata.setTitle("Cached title");
        metadata.setRating(7);

        writerService.writeMetadata(List.of(metadata));

        Map<Path, SoundFileMetadata> metadataByPath = readerService.readMetadata(List.of(song.toAbsolutePath().normalize()));

        SoundFileMetadata cachedMetadata = metadataByPath.get(song.toAbsolutePath().normalize());
        assertEquals("Cached title", cachedMetadata.title());
        assertEquals(7, cachedMetadata.rating());
    }

    @Test
    void skipsStaleMetadataWhenFileChangedAfterIndexWrite() throws IOException {
        Path indexPath = tempDir.resolve("metadata-index");
        Path song = Files.createFile(tempDir.resolve("song.mp3"));
        DocumentMapper documentMapper = new DocumentMapper();
        MetadataIndexWriterService writerService = new MetadataIndexWriterService(indexPath, documentMapper);
        MetadataIndexReaderService readerService = new MetadataIndexReaderService(indexPath, documentMapper);

        writerService.writeMetadata(List.of(SoundFileMetadata.empty(song)));
        Files.writeString(song, "changed");

        Map<Path, SoundFileMetadata> metadataByPath = readerService.readMetadata(List.of(song.toAbsolutePath().normalize()));

        assertFalse(metadataByPath.containsKey(song.toAbsolutePath().normalize()));
    }

    @Test
    void suggestsUniqueValuesByCaseInsensitivePrefix() throws IOException {
        Path indexPath = tempDir.resolve("metadata-index");
        Path firstSong = Files.createFile(tempDir.resolve("first.mp3"));
        Path secondSong = Files.createFile(tempDir.resolve("second.mp3"));
        Path thirdSong = Files.createFile(tempDir.resolve("third.mp3"));
        DocumentMapper documentMapper = new DocumentMapper();
        MetadataIndexWriterService writerService = new MetadataIndexWriterService(indexPath, documentMapper);
        MetadataIndexReaderService readerService = new MetadataIndexReaderService(indexPath, documentMapper);
        SoundFileMetadata firstMetadata = SoundFileMetadata.empty(firstSong);
        firstMetadata.setArtist("John Williams");
        SoundFileMetadata secondMetadata = SoundFileMetadata.empty(secondSong);
        secondMetadata.setArtist("Johnny Cash");
        SoundFileMetadata thirdMetadata = SoundFileMetadata.empty(thirdSong);
        thirdMetadata.setArtist("Miles Davis");

        writerService.writeMetadata(List.of(firstMetadata, secondMetadata, thirdMetadata));

        List<String> suggestions = readerService.suggestValues(Tag.ARTIST, "john", 10);

        assertEquals(List.of("John Williams", "Johnny Cash"), suggestions);
    }

    @Test
    void limitsSuggestedValues() throws IOException {
        Path indexPath = tempDir.resolve("metadata-index");
        Path firstSong = Files.createFile(tempDir.resolve("first.mp3"));
        Path secondSong = Files.createFile(tempDir.resolve("second.mp3"));
        DocumentMapper documentMapper = new DocumentMapper();
        MetadataIndexWriterService writerService = new MetadataIndexWriterService(indexPath, documentMapper);
        MetadataIndexReaderService readerService = new MetadataIndexReaderService(indexPath, documentMapper);
        SoundFileMetadata firstMetadata = SoundFileMetadata.empty(firstSong);
        firstMetadata.setGenre("Blues");
        SoundFileMetadata secondMetadata = SoundFileMetadata.empty(secondSong);
        secondMetadata.setGenre("Bluegrass");

        writerService.writeMetadata(List.of(firstMetadata, secondMetadata));

        List<String> suggestions = readerService.suggestValues(Tag.GENRE, "blu", 1);

        assertEquals(List.of("Bluegrass"), suggestions);
    }
}
