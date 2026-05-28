package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;

import java.nio.file.Path;

public class SoundFileMetadataMapper {
    private final MoodConverter moodConverter;

    public SoundFileMetadataMapper(MoodConverter moodConverter) {
        this.moodConverter = moodConverter;
    }

    public SoundFileMetadata toMetadata(AudioFile audioFile, Path indexedPath) {
        Path path = audioFile.getFile().toPath();
        AudioHeader audioHeader = audioFile.getAudioHeader();
        org.jaudiotagger.tag.Tag tag = audioFile.getTag();
        return new SoundFileMetadata(
                getFormat(audioHeader),
                path.toAbsolutePath().normalize().toString(),
                path.getFileName().toString(),
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

    private String getFormat(AudioHeader audioHeader) {
        String format = emptyToNull(audioHeader.getFormat());
        return format == null ? audioHeader.getEncodingType() : format;
    }

    private String getFirst(org.jaudiotagger.tag.Tag tag, FieldKey fieldKey) {
        if (tag == null) {
            return null;
        }

        return emptyToNull(tag.getFirst(fieldKey));
    }

    private String getMood(org.jaudiotagger.tag.Tag tag) {
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
