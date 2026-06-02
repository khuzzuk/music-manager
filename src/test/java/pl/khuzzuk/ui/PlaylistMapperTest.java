package pl.khuzzuk.ui;

import org.junit.jupiter.api.Test;
import pl.khuzzuk.player.SoundFile;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistMapperTest {
    @Test
    void writesOnlySoundFilePathsToSettingsValue() {
        String settingsValue = new PlaylistMapper().toSettingsValue(List.of(
                new SoundFile("C:\\Music\\first.mp3", "First"),
                new SoundFile("D:\\Archive\\second.flac", "Second")));

        assertEquals("C:\\Music\\first.mp3" + File.pathSeparator + "D:\\Archive\\second.flac", settingsValue);
    }

    @Test
    void readsSoundFilesFromSettingsValue() {
        List<SoundFile> soundFiles = new PlaylistMapper()
                .toSoundFiles("C:\\Music\\first.mp3" + File.pathSeparator + "D:\\Archive\\second.flac");

        assertEquals("C:\\Music\\first.mp3", soundFiles.get(0).path());
        assertEquals("first.mp3", soundFiles.get(0).title());
        assertEquals("D:\\Archive\\second.flac", soundFiles.get(1).path());
        assertEquals("second.flac", soundFiles.get(1).title());
    }
}
