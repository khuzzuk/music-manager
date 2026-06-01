package pl.khuzzuk.metadata;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MetadataIndexWriterService {
    private final Path indexDirectory;
    private final DocumentMapper documentMapper;

    public MetadataIndexWriterService(Path indexDirectory, DocumentMapper documentMapper) throws IOException {
        this.indexDirectory = indexDirectory;
        this.documentMapper = documentMapper;
        Files.createDirectories(indexDirectory);
    }

    public void writeMetadata(SoundFileMetadata metadata) throws IOException {
        writeMetadata(List.of(metadata));
    }

    public void writeMetadata(List<SoundFileMetadata> metadataItems) throws IOException {
        List<SoundFileMetadata> writableMetadataItems = metadataItems.stream()
                .filter(metadata -> metadata != null && metadata.path() != null)
                .toList();
        if (writableMetadataItems.isEmpty()) {
            return;
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
            for (SoundFileMetadata metadata : writableMetadataItems) {
                writer.updateDocument(
                        new Term(DocumentMapper.PATH_FIELD, metadata.path().toString()),
                        documentMapper.toDocument(metadata));
            }
        }
    }

    public void deleteMetadata(List<Path> paths) throws IOException {
        List<Path> deletablePaths = paths.stream()
                .filter(Objects::nonNull)
                .map(path -> path.toAbsolutePath().normalize())
                .toList();
        if (deletablePaths.isEmpty()) {
            return;
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory);
             IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer()))) {
            for (Path path : deletablePaths) {
                writer.deleteDocuments(new Term(DocumentMapper.PATH_FIELD, path.toString()));
            }
        }
    }
}
