package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.khuzzuk.player.SoundFileType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SoundFileMetadataMapperTest {
    @TempDir
    private Path tempDir;

    @Test
    void mapsJaudiotaggerFieldsToSoundFileMetadata() throws TagException {
        Path path = tempDir.resolve("song.mp3");
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.TITLE, "Song Title");
        tag.setField(FieldKey.RATING, "224");
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

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(path, "MP3", tag), tempDir);

        assertEquals(SoundFileType.MP3, metadata.format());
        assertEquals(path.toAbsolutePath().normalize(), metadata.path());
        assertEquals("song.mp3", metadata.fileName());
        assertEquals(tempDir.toAbsolutePath().normalize().toString(), metadata.indexedPath());
        assertEquals("Song Title", metadata.title());
        assertEquals(9, metadata.rating());
        assertEquals(125, metadata.durationSeconds());
        assertEquals("2:05", metadata.duration());
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

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(tempDir.resolve("song.mp3"), "MP3", tag), null);

        assertEquals("2026", metadata.date());
    }

    @Test
    void usesZeroRatingWhenRatingFieldIsNotNumeric() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.RATING, "invalid");

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(tempDir.resolve("song.mp3"), "MP3", tag), null);

        assertEquals(0, metadata.rating());
    }

    @Test
    void readsMp3RatingAsByteValue() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.RATING, "224");

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(tempDir.resolve("song.mp3"), "MP3", tag), null);

        assertEquals(9, metadata.rating());
    }

    @Test
    void readsFlacRatingAsPercentValue() throws TagException {
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.RATING, "80");

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(tempDir.resolve("song.flac"), "FLAC", tag), null);

        assertEquals(SoundFileType.FLAC, metadata.format());
        assertEquals(9, metadata.rating());
    }

    @Test
    void readsMoodThroughMoodConverter() {
        SoundFileMetadataMapper mapper = new SoundFileMetadataMapper(new MoodConverter() {
            @Override
            public String getMood(org.jaudiotagger.tag.Tag tag) {
                return "Energetic";
            }
        });

        SoundFileMetadata metadata = mapper.toMetadata(audioFile(tempDir.resolve("song.mp3"), "MP3", new ID3v24Tag()), null);

        assertEquals("Energetic", metadata.mood());
    }

    @Test
    void mapsMissingTagToMetadataWithFileValues() {
        Path path = tempDir.resolve("song.mp3");

        SoundFileMetadata metadata = mapper().toMetadata(audioFile(path, "MP3", null), null);

        assertEquals(SoundFileType.MP3, metadata.format());
        assertEquals(path.toAbsolutePath().normalize(), metadata.path());
        assertEquals("song.mp3", metadata.fileName());
        assertNull(metadata.indexedPath());
        assertNull(metadata.title());
        assertEquals(0, metadata.rating());
        assertEquals(125, metadata.durationSeconds());
        assertNull(metadata.date());
        assertNull(metadata.album());
        assertNull(metadata.mood());
    }

    private SoundFileMetadataMapper mapper() {
        return new SoundFileMetadataMapper(new MoodConverter());
    }

    private AudioFile audioFile(Path path, String format, org.jaudiotagger.tag.Tag tag) {
        return new AudioFile(path.toFile(), new TestAudioHeader(format), tag);
    }

    private record TestAudioHeader(String format) implements AudioHeader {
        @Override
        public String getEncodingType() {
            return format;
        }

        @Override
        public Integer getByteRate() {
            return 0;
        }

        @Override
        public String getBitRate() {
            return "";
        }

        @Override
        public long getBitRateAsNumber() {
            return 0;
        }

        @Override
        public Long getAudioDataLength() {
            return 0L;
        }

        @Override
        public Long getAudioDataStartPosition() {
            return 0L;
        }

        @Override
        public Long getAudioDataEndPosition() {
            return 0L;
        }

        @Override
        public String getSampleRate() {
            return "";
        }

        @Override
        public int getSampleRateAsNumber() {
            return 0;
        }

        @Override
        public String getFormat() {
            return format;
        }

        @Override
        public String getChannels() {
            return "";
        }

        @Override
        public boolean isVariableBitRate() {
            return false;
        }

        @Override
        public int getTrackLength() {
            return 125;
        }

        @Override
        public double getPreciseTrackLength() {
            return 124.7;
        }

        @Override
        public int getBitsPerSample() {
            return 0;
        }

        @Override
        public boolean isLossless() {
            return false;
        }

        @Override
        public Long getNoOfSamples() {
            return 0L;
        }
    }
}
