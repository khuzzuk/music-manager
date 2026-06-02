package pl.khuzzuk.ui;

import pl.khuzzuk.player.SoundFile;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class PlaylistMapper {
    String toSettingsValue(List<SoundFile> soundFiles) {
        return soundFiles.stream()
                .map(SoundFile::path)
                .collect(Collectors.joining(File.pathSeparator));
    }

    List<SoundFile> toSoundFiles(String playlistPaths) {
        if (playlistPaths == null || playlistPaths.isBlank()) {
            return List.of();
        }

        return Arrays.stream(playlistPaths.split(Pattern.quote(File.pathSeparator)))
                .filter(path -> !path.isBlank())
                .map(path -> new SoundFile(path, title(Path.of(path))))
                .toList();
    }

    private String title(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
