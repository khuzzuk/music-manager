package pl.khuzzuk.metadata;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;
import java.nio.file.Path;

public class MetadataReaderService {
    private final SoundFileMetadataMapper soundFileMetadataMapper;

    public MetadataReaderService() {
        this(new SoundFileMetadataMapper(new MoodConverter()));
    }

    public MetadataReaderService(SoundFileMetadataMapper soundFileMetadataMapper) {
        this.soundFileMetadataMapper = soundFileMetadataMapper;
    }

    public SoundFileMetadata readMetadata(Path path) throws IOException {
        return readMetadata(path, null);
    }

    public SoundFileMetadata readMetadata(Path path, Path indexedPath) throws IOException {
        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            return soundFileMetadataMapper.toMetadata(audioFile, indexedPath);
        } catch (CannotReadException | TagException | ReadOnlyFileException | InvalidAudioFrameException e) {
            throw new IOException("Cannot read audio metadata from " + path, e);
        }
    }
}
