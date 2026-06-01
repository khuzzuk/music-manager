package pl.khuzzuk.metadata;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MetadataIndexReaderService {
    private final Path indexDirectory;
    private final DocumentMapper documentMapper;

    public MetadataIndexReaderService(Path indexDirectory, DocumentMapper documentMapper) {
        this.indexDirectory = indexDirectory;
        this.documentMapper = documentMapper;
    }

    public Map<Path, SoundFileMetadata> readMetadata(List<Path> paths) throws IOException {
        if (paths == null || paths.isEmpty() || !Files.exists(indexDirectory)) {
            return Map.of();
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory)) {
            if (!DirectoryReader.indexExists(directory)) {
                return Map.of();
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Map<Path, SoundFileMetadata> metadataByPath = new HashMap<>();
                for (Path path : paths) {
                    Path normalizedPath = path.toAbsolutePath().normalize();
                    readMetadata(searcher, normalizedPath)
                            .ifPresent(metadata -> metadataByPath.put(normalizedPath, metadata));
                }
                return metadataByPath;
            }
        }
    }

    public List<String> suggestValues(Tag tag, String prefix, int limit) throws IOException {
        if (tag == null || tag.isNumeric() || prefix == null || prefix.isBlank() || limit <= 0
                || !Files.exists(indexDirectory)) {
            return List.of();
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory)) {
            if (!DirectoryReader.indexExists(directory)) {
                return List.of();
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(MatchAllDocsQuery.INSTANCE, Math.max(1, reader.numDocs()));
                String normalizedPrefix = prefix.trim().toLowerCase(Locale.ROOT);
                Set<String> values = new LinkedHashSet<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = reader.storedFields().document(scoreDoc.doc);
                    String value = document.get(tag.settingsName());
                    if (value == null || value.isBlank()) {
                        continue;
                    }

                    String trimmedValue = value.trim();
                    if (trimmedValue.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                        values.add(trimmedValue);
                    }
                }

                return values.stream()
                        .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                        .limit(limit)
                        .toList();
            }
        }
    }

    private Optional<SoundFileMetadata> readMetadata(IndexSearcher searcher, Path path) throws IOException {
        Path normalizedPath = path.toAbsolutePath().normalize();
        TopDocs topDocs = searcher.search(
                new TermQuery(new Term(DocumentMapper.PATH_FIELD, normalizedPath.toString())),
                1);
        if (topDocs.scoreDocs.length == 0) {
            return Optional.empty();
        }

        ScoreDoc scoreDoc = topDocs.scoreDocs[0];
        Document document = searcher.storedFields().document(scoreDoc.doc);
        if (!documentMapper.matchesFile(normalizedPath, document)) {
            return Optional.empty();
        }

        return Optional.of(documentMapper.toMetadata(document));
    }
}
