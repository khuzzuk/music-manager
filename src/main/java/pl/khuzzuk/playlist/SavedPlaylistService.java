package pl.khuzzuk.playlist;

import pl.khuzzuk.player.SoundFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SavedPlaylistService {
    private static final String PLAYLIST_EXTENSION = ".playlist";
    private static final Pattern INVALID_FILE_NAME_CHARS = Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");

    private final Path playlistsDirectory;
    private final SavedPlaylistMapper mapper;

    public SavedPlaylistService(Path playlistsDirectory, SavedPlaylistMapper mapper) throws IOException {
        this.playlistsDirectory = playlistsDirectory;
        this.mapper = mapper;
        Files.createDirectories(playlistsDirectory);
    }

    public List<SavedPlaylist> listPlaylists() throws IOException {
        if (Files.notExists(playlistsDirectory)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(playlistsDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(PLAYLIST_EXTENSION))
                    .map(path -> new SavedPlaylist(displayName(path), path))
                    .sorted(Comparator.comparing(SavedPlaylist::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    public SavedPlaylist savePlaylist(String name, List<SoundFile> soundFiles) throws IOException {
        String fileName = sanitizeName(name) + PLAYLIST_EXTENSION;
        Path playlistPath = playlistsDirectory.resolve(fileName);
        Files.write(playlistPath, mapper.toLines(soundFiles), StandardCharsets.UTF_8);
        return new SavedPlaylist(displayName(playlistPath), playlistPath);
    }

    public List<SoundFile> readPlaylist(SavedPlaylist playlist) throws IOException {
        return mapper.toSoundFiles(Files.readAllLines(playlist.path(), StandardCharsets.UTF_8));
    }

    public void deletePlaylist(SavedPlaylist playlist) throws IOException {
        Files.deleteIfExists(playlist.path());
    }

    private String displayName(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(PLAYLIST_EXTENSION)) {
            return fileName.substring(0, fileName.length() - PLAYLIST_EXTENSION.length());
        }

        return fileName;
    }

    private String sanitizeName(String name) {
        String sanitized = INVALID_FILE_NAME_CHARS.matcher(name == null ? "" : name.trim()).replaceAll("_");
        sanitized = sanitized.replaceAll("\\s+", " ");
        while (sanitized.endsWith(".") || sanitized.endsWith(" ")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.isBlank() ? "playlist" : sanitized;
    }
}
