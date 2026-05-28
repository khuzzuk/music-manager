package pl.khuzzuk.metadata;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

public class DocumentMapper {
    public static final String PATH_FIELD = "path";

    public Document toDocument(SoundFileMetadata metadata) {
        Document document = new Document();
        addString(document, PATH_FIELD, metadata.path());
        for (Tag tag : Tag.values()) {
            addTag(document, tag, metadata);
        }
        return document;
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
}
