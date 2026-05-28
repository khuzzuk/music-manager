package pl.khuzzuk.metadata;

import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetadataReaderServiceTest {
    @Test
    void mapsJaudiotaggerFieldsToSoundFileMetadata() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.TITLE, "Song Title");
        tag.setField(FieldKey.RATING, "80");
        tag.setField(FieldKey.RECORDINGDATE, "2026-05-28");
        tag.setField(FieldKey.ARTIST, "Artist Name");
        tag.setField(FieldKey.ARTISTS, "Artist One; Artist Two");
        tag.setField(FieldKey.ALBUM, "Album Name");
        tag.setField(FieldKey.ALBUM_ARTIST, "Album Artist");
        tag.setField(FieldKey.ALBUM_ARTISTS, "Album Artist One; Album Artist Two");
        tag.setField(FieldKey.COMPOSER, "Composer");
        tag.setField(FieldKey.CONDUCTOR, "Conductor");
        tag.setField(FieldKey.COUNTRY, "PL");
        tag.setField(FieldKey.CUSTOM1, "Custom 1");
        tag.setField(FieldKey.CUSTOM2, "Custom 2");
        tag.setField(FieldKey.CUSTOM3, "Custom 3");
        tag.setField(FieldKey.CUSTOM4, "Custom 4");
        tag.setField(FieldKey.CUSTOM5, "Custom 5");
        tag.setField(FieldKey.DISC_NO, "2");
        tag.setField(FieldKey.GENRE, "Rock");
        tag.setField(FieldKey.GROUP, "Group");
        tag.setField(FieldKey.INSTRUMENT, "Piano");
        tag.setField(FieldKey.MOOD, "Calm");
        tag.setField(FieldKey.MOVEMENT, "Allegro");
        tag.setField(FieldKey.OCCASION, "Concert");
        tag.setField(FieldKey.OPUS, "Op. 1");
        tag.setField(FieldKey.ORCHESTRA, "Orchestra");
        tag.setField(FieldKey.QUALITY, "Lossless");
        tag.setField(FieldKey.RANKING, "5");
        tag.setField(FieldKey.TEMPO, "120");
        tag.setField(FieldKey.TONALITY, "C");
        tag.setField(FieldKey.TRACK, "3");
        tag.setField(FieldKey.WORK, "Work");
        tag.setField(FieldKey.WORK_TYPE, "Symphony");

        SoundFileMetadata metadata = new MetadataReaderService().readMetadata(tag);

        assertNull(metadata.format());
        assertNull(metadata.path());
        assertNull(metadata.fileName());
        assertNull(metadata.indexedPath());
        assertEquals("Song Title", metadata.title());
        assertEquals(9, metadata.rating());
        assertEquals("2026-05-28", metadata.date());
        assertEquals("Artist Name", metadata.artist());
        assertEquals("Artist One; Artist Two", metadata.artists());
        assertEquals("Album Name", metadata.album());
        assertEquals("Album Artist", metadata.albumArtist());
        assertEquals("Album Artist One; Album Artist Two", metadata.albumArtists());
        assertEquals("Composer", metadata.composer());
        assertEquals("Conductor", metadata.conductor());
        assertEquals("PL", metadata.country());
        assertEquals("Custom 1", metadata.custom1());
        assertEquals("Custom 2", metadata.custom2());
        assertEquals("Custom 3", metadata.custom3());
        assertEquals("Custom 4", metadata.custom4());
        assertEquals("Custom 5", metadata.custom5());
        assertEquals("2", metadata.discNo());
        assertEquals("Rock", metadata.genre());
        assertEquals("Group", metadata.group());
        assertEquals("Piano", metadata.instrument());
        assertEquals("Calm", metadata.mood());
        assertEquals("Allegro", metadata.movement());
        assertEquals("Concert", metadata.occasion());
        assertEquals("Op. 1", metadata.opus());
        assertEquals("Orchestra", metadata.orchestra());
        assertEquals("Lossless", metadata.quality());
        assertEquals("5", metadata.ranking());
        assertEquals("120", metadata.tempo());
        assertEquals("C", metadata.tonality());
        assertEquals("3", metadata.track());
        assertEquals("Work", metadata.work());
        assertEquals("Symphony", metadata.workType());
    }

    @Test
    void usesYearAsDateFallback() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.YEAR, "2026");

        SoundFileMetadata metadata = new MetadataReaderService().readMetadata(tag);

        assertEquals("2026", metadata.date());
    }

    @Test
    void usesZeroRatingWhenRatingFieldIsNotNumeric() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.RATING, "invalid");

        SoundFileMetadata metadata = new MetadataReaderService().readMetadata(tag);

        assertEquals(0, metadata.rating());
    }

    @Test
    void readsMoodThroughMoodConverter() {
        ID3v24Tag tag = new ID3v24Tag();
        MetadataReaderService metadataReaderService = new MetadataReaderService(new MoodConverter() {
            @Override
            public String getMood(Tag tag) {
                return "Energetic";
            }
        });

        SoundFileMetadata metadata = metadataReaderService.readMetadata(tag);

        assertEquals("Energetic", metadata.mood());
    }

    @Test
    void returnsEmptyMetadataWhenTagIsMissing() {
        SoundFileMetadata metadata = new MetadataReaderService().readMetadata((Tag) null);

        assertNull(metadata.format());
        assertNull(metadata.path());
        assertNull(metadata.fileName());
        assertNull(metadata.indexedPath());
        assertNull(metadata.title());
        assertEquals(0, metadata.rating());
        assertNull(metadata.date());
        assertNull(metadata.album());
        assertNull(metadata.albumArtist());
        assertNull(metadata.albumArtists());
        assertNull(metadata.artist());
        assertNull(metadata.artists());
        assertNull(metadata.composer());
        assertNull(metadata.conductor());
        assertNull(metadata.country());
        assertNull(metadata.custom1());
        assertNull(metadata.custom2());
        assertNull(metadata.custom3());
        assertNull(metadata.custom4());
        assertNull(metadata.custom5());
        assertNull(metadata.discNo());
        assertNull(metadata.genre());
        assertNull(metadata.group());
        assertNull(metadata.instrument());
        assertNull(metadata.mood());
        assertNull(metadata.movement());
        assertNull(metadata.occasion());
        assertNull(metadata.opus());
        assertNull(metadata.orchestra());
        assertNull(metadata.quality());
        assertNull(metadata.ranking());
        assertNull(metadata.tempo());
        assertNull(metadata.tonality());
        assertNull(metadata.track());
        assertNull(metadata.work());
        assertNull(metadata.workType());
    }
}
