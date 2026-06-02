package pl.khuzzuk.metadata;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
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
import java.util.HashSet;
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

    public List<String> readValues(Tag tag) throws IOException {
        if (tag == null || !Files.exists(indexDirectory)) {
            return List.of();
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory)) {
            if (!DirectoryReader.indexExists(directory)) {
                return List.of();
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                TopDocs topDocs = new IndexSearcher(reader)
                        .search(MatchAllDocsQuery.INSTANCE, Math.max(1, reader.numDocs()));
                Set<String> values = new LinkedHashSet<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = reader.storedFields().document(scoreDoc.doc);
                    String value = readFieldValue(document, tag);
                    if (value != null && !value.isBlank()) {
                        values.add(value.trim());
                    }
                }

                return values.stream()
                        .sorted(Comparator.comparing(value -> value.toLowerCase(Locale.ROOT)))
                        .toList();
            }
        }
    }

    public List<Path> readPaths(Tag tag, List<String> values) throws IOException {
        if (!Files.exists(indexDirectory)) {
            return List.of();
        }

        Set<String> selectedValues = values == null
                ? Set.of()
                : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        if (tag == null && !selectedValues.isEmpty()) {
            return List.of();
        }

        try (FSDirectory directory = FSDirectory.open(indexDirectory)) {
            if (!DirectoryReader.indexExists(directory)) {
                return List.of();
            }

            try (DirectoryReader reader = DirectoryReader.open(directory)) {
                TopDocs topDocs = new IndexSearcher(reader)
                        .search(MatchAllDocsQuery.INSTANCE, Math.max(1, reader.numDocs()));
                List<Path> paths = new java.util.ArrayList<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document document = reader.storedFields().document(scoreDoc.doc);
                    if (!matchesValues(document, tag, selectedValues)) {
                        continue;
                    }

                    String path = document.get(DocumentMapper.PATH_FIELD);
                    if (path != null && !path.isBlank()) {
                        paths.add(Path.of(path).toAbsolutePath().normalize());
                    }
                }

                return paths.stream()
                        .sorted(Comparator.comparing(path -> path.toString().toLowerCase(Locale.ROOT)))
                        .toList();
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

    private boolean matchesValues(Document document, Tag tag, Set<String> selectedValues) {
        if (selectedValues.isEmpty()) {
            return true;
        }

        String value = readFieldValue(document, tag);
        return value != null && selectedValues.contains(value.trim());
    }

    private String readFieldValue(Document document, Tag tag) {
        if (tag == null) {
            return null;
        }

        IndexableField field = document.getField(tag.settingsName());
        if (field == null) {
            return null;
        }

        Number numericValue = field.numericValue();
        if (numericValue != null) {
            return Integer.toString(numericValue.intValue());
        }

        return field.stringValue();
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
