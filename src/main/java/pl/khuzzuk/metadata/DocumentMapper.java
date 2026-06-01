package pl.khuzzuk.metadata;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import pl.khuzzuk.player.SoundFileType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocumentMapper {
    public static final String PATH_FIELD = "path";
    public static final String INDEXED_PATH_FIELD = "indexedPath";
    public static final String LAST_MODIFIED_FIELD = "lastModified";
    public static final String SIZE_FIELD = "size";
    public static final String DURATION_SECONDS_FIELD = "durationSeconds";

    public Document toDocument(SoundFileMetadata metadata) {
        Document document = new Document();
        addString(document, PATH_FIELD, metadata.path() == null ? null : metadata.path().toString());
        addString(document, INDEXED_PATH_FIELD, metadata.indexedPath());
        addFileStats(document, metadata.path());
        document.add(new StoredField(DURATION_SECONDS_FIELD, metadata.durationSeconds()));
        for (Tag tag : Tag.values()) {
            addTag(document, tag, metadata);
        }
        return document;
    }

    public SoundFileMetadata toMetadata(Document document) {
        Path path = Path.of(document.get(PATH_FIELD));
        SoundFileType format = readFormat(document, path);
        return new SoundFileMetadata(
                format,
                path.toAbsolutePath().normalize(),
                path.getFileName().toString(),
                document.get(INDEXED_PATH_FIELD),
                document.get(Tag.TITLE.settingsName()),
                readInt(document, Tag.RATING.settingsName()),
                readInt(document, DURATION_SECONDS_FIELD),
                document.get(Tag.DATE.settingsName()),
                document.get(Tag.ARTIST.settingsName()),
                document.get(Tag.ARTISTS.settingsName()),
                document.get(Tag.ALBUM.settingsName()),
                document.get(Tag.ALBUM_ARTIST.settingsName()),
                document.get(Tag.ALBUM_ARTISTS.settingsName()),
                document.get(Tag.COMPOSER.settingsName()),
                document.get(Tag.CONDUCTOR.settingsName()),
                document.get(Tag.COUNTRY.settingsName()),
                document.get(Tag.CUSTOM1.settingsName()),
                document.get(Tag.CUSTOM2.settingsName()),
                document.get(Tag.CUSTOM3.settingsName()),
                document.get(Tag.CUSTOM4.settingsName()),
                document.get(Tag.CUSTOM5.settingsName()),
                document.get(Tag.DISC_NO.settingsName()),
                document.get(Tag.GENRE.settingsName()),
                document.get(Tag.GROUP.settingsName()),
                document.get(Tag.INSTRUMENT.settingsName()),
                document.get(Tag.MOOD.settingsName()),
                document.get(Tag.MOVEMENT.settingsName()),
                document.get(Tag.OCCASION.settingsName()),
                document.get(Tag.OPUS.settingsName()),
                document.get(Tag.ORCHESTRA.settingsName()),
                document.get(Tag.QUALITY.settingsName()),
                document.get(Tag.RANKING.settingsName()),
                document.get(Tag.TEMPO.settingsName()),
                document.get(Tag.TONALITY.settingsName()),
                document.get(Tag.TRACK.settingsName()),
                document.get(Tag.WORK.settingsName()),
                document.get(Tag.WORK_TYPE.settingsName()));
    }

    public boolean matchesFile(Path path, Document document) {
        Number indexedLastModified = numericValue(document, LAST_MODIFIED_FIELD);
        Number indexedSize = numericValue(document, SIZE_FIELD);
        if (indexedLastModified == null || indexedSize == null) {
            return false;
        }

        try {
            return Files.getLastModifiedTime(path).toMillis() == indexedLastModified.longValue()
                    && Files.size(path) == indexedSize.longValue();
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    private void addTag(Document document, Tag tag, SoundFileMetadata metadata) {
        Object value = tag.getValue(metadata);
        if (value == null) {
            return;
        }

        if (tag.isNumeric() && value instanceof Number number) {
            document.add(new StoredField(tag.settingsName(), number.intValue()));
            return;
        }

        addText(document, tag.settingsName(), value.toString());
    }

    private void addString(Document document, String name, String value) {
        if (value != null) {
            document.add(new StringField(name, value, StringField.Store.YES));
        }
    }

    private void addText(Document document, String name, String value) {
        if (value != null) {
            document.add(new TextField(name, value, TextField.Store.YES));
        }
    }

    private void addFileStats(Document document, Path path) {
        if (path == null) {
            return;
        }

        try {
            document.add(new LongField(LAST_MODIFIED_FIELD, Files.getLastModifiedTime(path).toMillis(), LongField.Store.YES));
            document.add(new LongField(SIZE_FIELD, Files.size(path), LongField.Store.YES));
        } catch (IOException | SecurityException e) {
            // Missing file stats make the cached document unusable for fast reads.
        }
    }

    private SoundFileType readFormat(Document document, Path path) {
        String formatValue = document.get(Tag.FORMAT.settingsName());
        if (formatValue != null && !formatValue.isBlank()) {
            return SoundFileType.valueOf(formatValue.trim());
        }

        return SoundFileType.fromPath(path)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sound file path: " + path));
    }

    private int readInt(Document document, String fieldName) {
        Number value = numericValue(document, fieldName);
        return value == null ? 0 : value.intValue();
    }

    private Number numericValue(Document document, String fieldName) {
        return document.getField(fieldName) == null ? null : document.getField(fieldName).numericValue();
    }
}
