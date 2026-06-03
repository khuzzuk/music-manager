package pl.khuzzuk.settings;

import pl.khuzzuk.metadata.Tag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsToPropertiesMapperTest {
    @Test
    void readsTrackColumnsFromSettingsProperties() {
        Properties properties = new Properties();
        properties.setProperty("track.columns", "title:250;album:180;rating:70");

        Settings settings = new SettingsToPropertiesMapper().toSettings(properties);

        assertEquals(
                List.of(
                        new TrackColumn(Tag.TITLE, 250),
                        new TrackColumn(Tag.ALBUM, 180),
                        new TrackColumn(Tag.RATING, 70),
                        new TrackColumn(Tag.FILE_NAME, 240),
                        new TrackColumn(Tag.DURATION, 80),
                        new TrackColumn(Tag.COMPOSER, 160),
                        new TrackColumn(Tag.MOOD, 120),
                        new TrackColumn(Tag.TEMPO, 120),
                        new TrackColumn(Tag.OCCASION, 120)),
                settings.trackColumns());
    }

    @Test
    void usesDefaultTrackColumnsWhenPropertyIsMissing() {
        Settings settings = new SettingsToPropertiesMapper().toSettings(new Properties());

        assertEquals(SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS, settings.trackColumns());
    }

    @Test
    void writesTrackColumnsToSettingsProperties() {
        Settings settings = new Settings(
                100,
                100,
                600,
                400,
                false,
                "",
                "",
                2,
                List.of(),
                null,
                List.of(new TrackColumn(Tag.MOOD, 140), new TrackColumn(Tag.OCCASION, 160)),
                List.of(new TrackSort(Tag.RATING, TrackSortDirection.DESCENDING)),
                Tag.COMPOSER);

        Properties properties = new SettingsToPropertiesMapper().toProperties(settings);

        assertEquals("mood:140;occasion:160", properties.getProperty("track.columns"));
        assertEquals("rating:descending", properties.getProperty("track.sort"));
        assertEquals("composer", properties.getProperty("last.tracks.filter.tag"));
        assertEquals("2", properties.getProperty("last.playlist.position"));
    }

    @Test
    void readsLastTracksFilterTagFromSettingsProperties() {
        Properties properties = new Properties();
        properties.setProperty("last.tracks.filter.tag", "albumArtist");

        Settings settings = new SettingsToPropertiesMapper().toSettings(properties);

        assertEquals(Tag.ALBUM_ARTIST, settings.lastTracksFilterTag());
    }

    @Test
    void usesMoodAsDefaultTracksFilterTag() {
        Settings settings = new SettingsToPropertiesMapper().toSettings(new Properties());

        assertEquals(Tag.MOOD, settings.lastTracksFilterTag());
    }

    @Test
    void readsLastPlaylistPositionFromSettingsProperties() {
        Properties properties = new Properties();
        properties.setProperty("last.playlist.position", "3");

        Settings settings = new SettingsToPropertiesMapper().toSettings(properties);

        assertEquals(3, settings.lastPlaylistPosition());
    }

    @Test
    void usesMinusOneAsDefaultPlaylistPosition() {
        Settings settings = new SettingsToPropertiesMapper().toSettings(new Properties());

        assertEquals(-1, settings.lastPlaylistPosition());
    }

    @Test
    void readsTrackSortFromSettingsProperties() {
        Properties properties = new Properties();
        properties.setProperty("track.sort", "title:ascending;rating:descending;unknown:ascending;album:invalid");

        Settings settings = new SettingsToPropertiesMapper().toSettings(properties);

        assertEquals(
                List.of(
                        new TrackSort(Tag.TITLE, TrackSortDirection.ASCENDING),
                        new TrackSort(Tag.RATING, TrackSortDirection.DESCENDING)),
                settings.trackSort());
    }

    @Test
    void usesEmptyTrackSortWhenPropertyIsMissing() {
        Settings settings = new SettingsToPropertiesMapper().toSettings(new Properties());

        assertEquals(List.of(), settings.trackSort());
    }
}
