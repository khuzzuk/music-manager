package pl.khuzzuk.metadata;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;
import pl.khuzzuk.player.SoundFileType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentMapperTest {
    @Test
    void mapsSoundFileMetadataToLuceneDocument() {
        Path song = Path.of("song.mp3").toAbsolutePath().normalize();
        SoundFileMetadata metadata = new SoundFileMetadata(
                SoundFileType.MP3,
                song.toString(),
                "song.mp3",
                null,
                "Title",
                9,
                "2026",
                "Artist",
                null,
                "Album",
                null,
                null,
                "Composer",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Rock",
                null,
                null,
                "Calm",
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

        Document document = new DocumentMapper().toDocument(metadata);

        assertEquals(song.toString(), document.get(DocumentMapper.PATH_FIELD));
        assertEquals("Title", document.get(Tag.TITLE.settingsName()));
        assertEquals("Album", document.get(Tag.ALBUM.settingsName()));
        assertEquals("Composer", document.get(Tag.COMPOSER.settingsName()));
        assertEquals("Calm", document.get(Tag.MOOD.settingsName()));
        assertEquals(9, document.getField(Tag.RATING.settingsName()).numericValue().intValue());
    }
}
