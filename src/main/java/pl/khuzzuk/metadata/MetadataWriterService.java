package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.file.Path;

public class MetadataWriterService {
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
                 | ReadOnlyFileException
                 | InvalidAudioFrameException e) {
            throw new IOException("Cannot write audio metadata to " + path, e);
        }
    }
}
