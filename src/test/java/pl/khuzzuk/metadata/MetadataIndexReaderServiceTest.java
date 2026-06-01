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
}
