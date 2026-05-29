package pl.khuzzuk.metadata;

import org.junit.jupiter.api.Test;
import pl.khuzzuk.player.SoundFileType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SoundFileMetadataUpdateMapperTest {
    private final SoundFileMetadataUpdateMapper mapper = new SoundFileMetadataUpdateMapper();

    @Test
    void updatesSelectedTextFieldWithoutChangingOtherValues() {
        SoundFileMetadata metadata = metadata();

        mapper.setValue(metadata, Tag.ALBUM, "New album");

        assertEquals("Title", metadata.getTitle());
        assertEquals("New album", metadata.getAlbum());
        assertEquals("Artist", metadata.getArtist());
        assertEquals(8, metadata.getRating());
    }

    @Test
    void canClearTextField() {
        SoundFileMetadata metadata = metadata();

        mapper.setValue(metadata, Tag.MOOD, null);

        assertNull(metadata.getMood());
    }

    @Test
    void updatesRating() {
        SoundFileMetadata metadata = metadata();

        mapper.setValue(metadata, Tag.RATING, "10");

        assertEquals(10, metadata.getRating());
    }

    @Test
    void updatesFormat() {
        SoundFileMetadata metadata = metadata();

        mapper.setValue(metadata, Tag.FORMAT, "FLAC");

        assertSame(SoundFileType.FLAC, metadata.getFormat());
    }

    private SoundFileMetadata metadata() {
        return new SoundFileMetadata(
                SoundFileType.MP3,
                "C:\\music\\song.mp3",
                "song.mp3",
                null,
                "Title",
                8,
                125,
                "2026",
                "Artist",
                null,
                "Album",
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
    }
}
