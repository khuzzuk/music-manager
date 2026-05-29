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
                        new TrackColumn(Tag.DURATION, 80),
                        new TrackColumn(Tag.COMPOSER, 160),
                        new TrackColumn(Tag.MOOD, 120),
                        new TrackColumn(Tag.MOVEMENT, 120),
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
                List.of(),
                null,
                List.of(new TrackColumn(Tag.MOOD, 140), new TrackColumn(Tag.OCCASION, 160)));

        Properties properties = new SettingsToPropertiesMapper().toProperties(settings);

        assertEquals("mood:140;occasion:160", properties.getProperty("track.columns"));
    }
}
