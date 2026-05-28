package pl.khuzzuk.metadata;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MetadataIndexWriterService {
    private final Path indexDirectory;
    private final DocumentMapper documentMapper;

    public MetadataIndexWriterService(Path indexDirectory, DocumentMapper documentMapper) throws IOException {
        this.indexDirectory = indexDirectory;
        this.documentMapper = documentMapper;
        Files.createDirectories(indexDirectory);
    }

    public void writeMetadata(SoundFileMetadata metadata) throws IOException {
        if (metadata == null || metadata.path() == null || metadata.path().isBlank()) {
            return;
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
            writer.updateDocument(
                    new Term(DocumentMapper.PATH_FIELD, metadata.path()),
                    documentMapper.toDocument(metadata));
        }
    }
}
