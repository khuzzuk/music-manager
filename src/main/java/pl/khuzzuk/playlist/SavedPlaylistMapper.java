package pl.khuzzuk.playlist;

import pl.khuzzuk.player.SoundFile;

import java.nio.file.Path;
import java.util.List;

public class SavedPlaylistMapper {
    public List<String> toLines(List<SoundFile> soundFiles) {
        return soundFiles.stream()
                .map(SoundFile::path)
                .toList();
    }

    public List<SoundFile> toSoundFiles(List<String> lines) {
        return lines.stream()
                .filter(line -> line != null && !line.isBlank())
                .map(path -> new SoundFile(path, title(Path.of(path))))
                .toList();
    }

    private String title(Path path) {
        Path fileName = path.getFileName();
        return fileName == null ? path.toString() : fileName.toString();
    }
}
