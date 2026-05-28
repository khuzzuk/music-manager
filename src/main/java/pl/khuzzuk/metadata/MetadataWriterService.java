package pl.khuzzuk.metadata;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetadataWriterService {
    private final Path indexDirectory;
    private final DocumentMapper documentMapper;

    public MetadataWriterService(Path indexDirectory, DocumentMapper documentMapper) {
        this.indexDirectory = indexDirectory;
        this.documentMapper = documentMapper;
    }

    public void writeMetadata(SoundFileMetadata metadata) throws IOException {
        if (metadata == null || metadata.path() == null || metadata.path().isBlank()) {
            return;
        }

        Files.createDirectories(indexDirectory);
        try (FSDirectory directory = FSDirectory.open(indexDirectory);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
            writer.updateDocument(
                    new Term(DocumentMapper.PATH_FIELD, metadata.path()),
                    documentMapper.toDocument(metadata));
        }
    }
}
