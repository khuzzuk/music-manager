package pl.khuzzuk.metadata;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataIndexWriterServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void writesMetadataToLuceneIndexAndUpdatesExistingPath() throws IOException {
        Path indexPath = tempDir.resolve("metadata-index");
        Path song = tempDir.resolve("song.mp3");
        MetadataIndexWriterService metadataIndexWriterService = new MetadataIndexWriterService(
                indexPath,
                new DocumentMapper());
        SoundFileMetadata firstMetadata = metadata(song, "First title", 8);
        SoundFileMetadata updatedMetadata = metadata(song, "Updated title", 10);

        metadataIndexWriterService.writeMetadata(firstMetadata);
        metadataIndexWriterService.writeMetadata(updatedMetadata);

        try (FSDirectory directory = FSDirectory.open(indexPath);
             DirectoryReader reader = DirectoryReader.open(directory)) {
            assertEquals(1, reader.numDocs());
            org.apache.lucene.document.Document document = reader.storedFields().document(0);
            assertEquals(song.toAbsolutePath().normalize().toString(), document.get("path"));
            assertEquals("Updated title", document.get(Tag.TITLE.settingsName()));
            assertEquals(10, document.getField(Tag.RATING.settingsName()).numericValue().intValue());
        }
    }

    private SoundFileMetadata metadata(Path path, String title, int rating) {
        return new SoundFileMetadata(
                SoundFileType.MP3,
                path.toAbsolutePath().normalize().toString(),
                path.getFileName().toString(),
                null,
                title,
                rating,
                125,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
