package pl.khuzzuk.metadata;

import pl.khuzzuk.player.SoundFileType;

import java.nio.file.Path;
import java.util.Objects;

public record SoundFileMetadata(
        SoundFileType format,
        String path,
        String fileName,
        String indexedPath,
        String title,
        int rating,
        String date,
        String artist,
        String artists,
        String album,
        String albumArtist,
        String albumArtists,
        String composer,
        String conductor,
        String country,
        String custom1,
        String custom2,
        String custom3,
        String custom4,
        String custom5,
        String discNo,
        String genre,
        String group,
        String instrument,
        String mood,
        String movement,
        String occasion,
        String opus,
        String orchestra,
        String quality,
        String ranking,
        String tempo,
        String tonality,
        String track,
        String work,
        String workType) {
    public SoundFileMetadata {
        Objects.requireNonNull(format, "format");
    }

    public static SoundFileMetadata empty(Path path) {
        return empty(path, null);
    }

    public static SoundFileMetadata empty(Path path, Path indexedPath) {
        SoundFileType format = SoundFileType.fromPath(path)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sound file path: " + path));
        return new SoundFileMetadata(
                format,
                path.toAbsolutePath().normalize().toString(),
                path.getFileName().toString(),
                indexedPath == null ? null : indexedPath.toAbsolutePath().normalize().toString(),
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public SoundFileMetadata withRating(int newRating) {
        return new SoundFileMetadata(
                format,
                path,
                fileName,
                indexedPath,
                title,
                newRating,
                date,
                artist,
                artists,
                album,
                albumArtist,
                albumArtists,
                composer,
                conductor,
                country,
                custom1,
                custom2,
                custom3,
                custom4,
                custom5,
                discNo,
                genre,
                group,
                instrument,
                mood,
                movement,
                occasion,
                opus,
                orchestra,
                quality,
                ranking,
                tempo,
                tonality,
                track,
                work,
                workType);
    }
}
