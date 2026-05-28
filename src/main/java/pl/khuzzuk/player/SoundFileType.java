package pl.khuzzuk.player;

import pl.khuzzuk.metadata.Rating;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public enum SoundFileType {
    MP3(Set.of("mp3"), true),
    FLAC(Set.of("flac"), false);

    private final Set<String> extensions;
    private final boolean byteRating;

    SoundFileType(Set<String> extensions, boolean byteRating) {
        this.extensions = extensions;
        this.byteRating = byteRating;
    }

    public static Optional<SoundFileType> fromPath(Path path) {
        if (path == null || path.getFileName() == null) {
            return Optional.empty();
        }

        String fileName = path.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
            return Optional.empty();
        }

        return fromExtension(fileName.substring(extensionStart + 1));
    }

    public static Optional<SoundFileType> fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }

        String normalizedExtension = extension.trim().toLowerCase(Locale.ROOT);
        return Stream.of(values())
                .filter(type -> type.extensions.contains(normalizedExtension))
                .findFirst();
    }

    public int readRating(String value) {
        if (value == null || value.isBlank()) {
            return Rating.UNDEFINED.getRate();
        }

        try {
            int parsedValue = Integer.parseInt(value.trim());
            return byteRating
                    ? Rating.fromByte(parsedValue).getRate()
                    : Rating.fromP(parsedValue).getRate();
        } catch (NumberFormatException e) {
            return Rating.UNDEFINED.getRate();
        }
    }

    public int getRatingMetadataValue(int ratingValue) {
        Rating rating = Rating.fromRate(ratingValue);
        return byteRating ? rating.getValue() : rating.getValueP();
    }
}
