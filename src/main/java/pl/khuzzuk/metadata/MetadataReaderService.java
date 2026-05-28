package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;
import java.nio.file.Path;

public class MetadataReaderService {
    private final MoodConverter moodConverter;

    public MetadataReaderService() {
        this(new MoodConverter());
    }

    MetadataReaderService(MoodConverter moodConverter) {
        this.moodConverter = moodConverter;
    }

    public SoundFileMetadata readMetadata(Path path) throws IOException {
        return readMetadata(path, null);
    }

    public SoundFileMetadata readMetadata(Path path, Path indexedPath) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            return readMetadata(audioFile, indexedPath);
        } catch (CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            throw new IOException("Cannot read audio metadata from " + path, e);
        }
    }

    SoundFileMetadata readMetadata(Tag tag) {
        return readMetadata(null, null, null, tag);
    }

    private SoundFileMetadata readMetadata(AudioFile audioFile, Path indexedPath) {
        Path path = audioFile.getFile().toPath();
        AudioHeader audioHeader = audioFile.getAudioHeader();
        return readMetadata(
                emptyToNull(audioHeader == null ? null : audioHeader.getFormat()),
                path,
                indexedPath,
                audioFile.getTag());
    }

    private SoundFileMetadata readMetadata(String format, Path path, Path indexedPath, Tag tag) {
        if (format == null && path == null && indexedPath == null && tag == null) {
            return SoundFileMetadata.empty(null);
        }

        return new SoundFileMetadata(
                format,
                path == null ? null : path.toAbsolutePath().normalize().toString(),
                path == null ? null : path.getFileName().toString(),
                indexedPath == null ? null : indexedPath.toAbsolutePath().normalize().toString(),
                getFirst(tag, FieldKey.TITLE),
                Rating.fromMetadataValue(getFirst(tag, FieldKey.RATING)),
                firstNotNull(getFirst(tag, FieldKey.RECORDINGDATE), getFirst(tag, FieldKey.YEAR)),
                getFirst(tag, FieldKey.ARTIST),
                getFirst(tag, FieldKey.ARTISTS),
                getFirst(tag, FieldKey.ALBUM),
                getFirst(tag, FieldKey.ALBUM_ARTIST),
                getFirst(tag, FieldKey.ALBUM_ARTISTS),
                getFirst(tag, FieldKey.COMPOSER),
                getFirst(tag, FieldKey.CONDUCTOR),
                getFirst(tag, FieldKey.COUNTRY),
                getFirst(tag, FieldKey.CUSTOM1),
                getFirst(tag, FieldKey.CUSTOM2),
                getFirst(tag, FieldKey.CUSTOM3),
                getFirst(tag, FieldKey.CUSTOM4),
                getFirst(tag, FieldKey.CUSTOM5),
                getFirst(tag, FieldKey.DISC_NO),
                getFirst(tag, FieldKey.GENRE),
                firstNotNull(getFirst(tag, FieldKey.GROUP), getFirst(tag, FieldKey.GROUPING)),
                getFirst(tag, FieldKey.INSTRUMENT),
                getMood(tag),
                getFirst(tag, FieldKey.MOVEMENT),
                getFirst(tag, FieldKey.OCCASION),
                getFirst(tag, FieldKey.OPUS),
                getFirst(tag, FieldKey.ORCHESTRA),
                getFirst(tag, FieldKey.QUALITY),
                getFirst(tag, FieldKey.RANKING),
                getFirst(tag, FieldKey.TEMPO),
                getFirst(tag, FieldKey.TONALITY),
                getFirst(tag, FieldKey.TRACK),
                getFirst(tag, FieldKey.WORK),
                getFirst(tag, FieldKey.WORK_TYPE));
    }

    private String getFirst(Tag tag, FieldKey fieldKey) {
        if (tag == null) {
            return null;
        }

        return emptyToNull(tag.getFirst(fieldKey));
    }

    private String getMood(Tag tag) {
        if (tag == null) {
            return null;
        }

        return emptyToNull(moodConverter.getMood(tag));
    }

    private String firstNotNull(String first, String second) {
        return first == null ? second : first;
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }
}
