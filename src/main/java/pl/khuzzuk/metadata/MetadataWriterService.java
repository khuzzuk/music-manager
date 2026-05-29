package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.TagException;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.file.Path;

public class MetadataWriterService {
    private final MetadataFieldKeyMapper metadataFieldKeyMapper;

    public MetadataWriterService(MetadataFieldKeyMapper metadataFieldKeyMapper) {
        this.metadataFieldKeyMapper = metadataFieldKeyMapper;
    }

    public boolean canWrite(Tag tag) {
        return metadataFieldKeyMapper.toFieldKey(tag) != null;
    }

    public void writeTag(Path path, Tag metadataTag, String value) throws IOException {
        if (metadataTag == Tag.RATING) {
            writeRating(path, parseRating(value));
            return;
        }

        FieldKey fieldKey = metadataFieldKeyMapper.toFieldKey(metadataTag);
        if (fieldKey == null) {
            throw new IOException("Unsupported metadata tag for writing: " + metadataTag);
        }

        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            org.jaudiotagger.tag.Tag tag = audioFile.getTagOrCreateAndSetDefault();
            if (value == null || value.isBlank()) {
                tag.deleteField(fieldKey);
            } else {
                tag.setField(fieldKey, value);
            }
            AudioFileIO.write(audioFile);
        } catch (CannotReadException
                 | CannotWriteException
                 | TagException
                 | KeyNotFoundException
                 | ReadOnlyFileException
                 | InvalidAudioFrameException e) {
            throw new IOException("Cannot write audio metadata to " + path, e);
        }
    }

    public void writeRating(Path path, int ratingValue) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            org.jaudiotagger.tag.Tag tag = audioFile.getTagOrCreateAndSetDefault();
            int metadataValue = SoundFileType.fromPath(path)
                    .orElseThrow(() -> new IOException("Unsupported sound file path: " + path))
                    .getRatingMetadataValue(ratingValue);
            tag.setField(FieldKey.RATING, String.valueOf(metadataValue));
            AudioFileIO.write(audioFile);
        } catch (CannotReadException
                 | CannotWriteException
                 | TagException
                 | KeyNotFoundException
                 | ReadOnlyFileException
                 | InvalidAudioFrameException e) {
            throw new IOException("Cannot write audio metadata to " + path, e);
        }
    }

    private int parseRating(String value) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid rating value: " + value, e);
        }
    }
}
