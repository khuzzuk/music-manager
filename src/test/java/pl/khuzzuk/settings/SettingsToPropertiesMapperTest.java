package pl.khuzzuk.settings;

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
                        new TrackColumn("title", 250),
                        new TrackColumn("album", 180),
                        new TrackColumn("rating", 70)),
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
                List.of(new TrackColumn("mood", 140), new TrackColumn("occasion", 160)));

        Properties properties = new SettingsToPropertiesMapper().toProperties(settings);

        assertEquals("mood:140;occasion:160", properties.getProperty("track.columns"));
    }
}
