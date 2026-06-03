package pl.khuzzuk.playlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.khuzzuk.player.SoundFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedPlaylistServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesEachPlaylistAsSeparateFileAndReadsSoundFiles() throws Exception {
        SavedPlaylistService service = new SavedPlaylistService(tempDir, new SavedPlaylistMapper());
        SavedPlaylist playlist = service.savePlaylist("Evening Scores", List.of(
                new SoundFile("C:\\Music\\first.mp3", "First"),
                new SoundFile("D:\\Archive\\second.flac", "Second")));

        assertTrue(Files.exists(tempDir.resolve("Evening Scores.playlist")));
        assertEquals("Evening Scores", playlist.name());

        List<SoundFile> soundFiles = service.readPlaylist(playlist);

        assertEquals("C:\\Music\\first.mp3", soundFiles.get(0).path());
        assertEquals("first.mp3", soundFiles.get(0).title());
        assertEquals("D:\\Archive\\second.flac", soundFiles.get(1).path());
        assertEquals("second.flac", soundFiles.get(1).title());
    }

    @Test
    void listsPlaylistsByDisplayName() throws Exception {
        SavedPlaylistService service = new SavedPlaylistService(tempDir, new SavedPlaylistMapper());
        service.savePlaylist("Zeta", List.of(new SoundFile("z.mp3", "z")));
        service.savePlaylist("alpha", List.of(new SoundFile("a.mp3", "a")));

        List<SavedPlaylist> playlists = service.listPlaylists();

        assertEquals("alpha", playlists.get(0).name());
        assertEquals("Zeta", playlists.get(1).name());
    }

    @Test
    void sanitizesPlaylistNameForFileSystem() throws Exception {
        SavedPlaylistService service = new SavedPlaylistService(tempDir, new SavedPlaylistMapper());

        service.savePlaylist("Film: Scores / Night?", List.of(new SoundFile("track.mp3", "Track")));

        assertTrue(Files.exists(tempDir.resolve("Film_ Scores _ Night_.playlist")));
    }

    @Test
    void deletesSavedPlaylistFile() throws Exception {
        SavedPlaylistService service = new SavedPlaylistService(tempDir, new SavedPlaylistMapper());
        SavedPlaylist playlist = service.savePlaylist("Archive", List.of(new SoundFile("track.mp3", "Track")));

        service.deletePlaylist(playlist);

        assertTrue(service.listPlaylists().isEmpty());
    }
}
