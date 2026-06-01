package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.SoundFileMetadata;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class TracksTableController {
    private final MetadataReaderService metadataReaderService;
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private SwingWorker<List<SoundFileMetadata>, Void> loadWorker;
    private int loadGeneration;

    TracksTableController(Context context) {
        this.metadataReaderService = context.metadataReaderService();
        this.metadataIndexReaderService = context.metadataIndexReaderService();
        this.metadataIndexWriterService = context.metadataIndexWriterService();
    }

    void loadFiles(
            List<IndexItem> files,
            BiConsumer<Integer, Integer> progressConsumer,
            Consumer<List<SoundFileMetadata>> loadedConsumer,
            Consumer<Exception> errorConsumer) {
        int generation = ++loadGeneration;
        cancelLoadWorker();

        if (files == null || files.isEmpty()) {
            loadedConsumer.accept(List.of());
            return;
        }

        List<IndexItem> filesSnapshot = List.copyOf(files);
        notifyProgress(progressConsumer, 0, filesSnapshot.size());
        loadWorker = new SwingWorker<>() {
            @Override
            protected List<SoundFileMetadata> doInBackground() {
                List<SoundFileMetadata> loadedTracks = new ArrayList<>();
                Map<Path, SoundFileMetadata> indexedMetadata = readMetadataIndex(filesSnapshot);
                for (int i = 0; i < filesSnapshot.size(); i++) {
                    if (isCancelled()) {
                        return List.of();
                    }

                    IndexItem file = filesSnapshot.get(i);
                    SoundFileMetadata metadata;
                    if (file instanceof SoundFileIndexItem soundFileIndexItem) {
                        metadata = readMetadata(soundFileIndexItem, indexedMetadata);
                    } else {
                        metadata = SoundFileMetadata.empty(file.getPath());
                    }

                    loadedTracks.add(metadata);
                    notifyProgress(progressConsumer, i + 1, filesSnapshot.size());
                }

                return loadedTracks;
            }

            @Override
            protected void done() {
                if (isCancelled() || generation != loadGeneration) {
                    return;
                }

                try {
                    loadedConsumer.accept(get());
                } catch (Exception e) {
                    loadedConsumer.accept(List.of());
                    errorConsumer.accept(e);
                }
            }
        };
        loadWorker.execute();
    }

    private void cancelLoadWorker() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
    }

    private SoundFileMetadata readMetadata(
            SoundFileIndexItem soundFileIndexItem,
            Map<Path, SoundFileMetadata> indexedMetadata) {
        SoundFileMetadata metadata = indexedMetadata.get(soundFileIndexItem.getPath());
        if (metadata != null) {
            return metadata;
        }

        try {
            metadata = metadataReaderService.readMetadata(soundFileIndexItem.getPath());
            writeMetadataIndex(metadata);
            return metadata;
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot read metadata while loading table: " + soundFileIndexItem.getPath(), e);
            return SoundFileMetadata.empty(soundFileIndexItem.getPath());
        }
    }

    private Map<Path, SoundFileMetadata> readMetadataIndex(List<IndexItem> files) {
        List<Path> soundFilePaths = files.stream()
                .filter(SoundFileIndexItem.class::isInstance)
                .map(IndexItem::getPath)
                .toList();
        try {
            return metadataIndexReaderService.readMetadata(soundFilePaths);
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot read metadata index while loading table.", e);
            return Map.of();
        }
    }

    private void writeMetadataIndex(SoundFileMetadata metadata) {
        try {
            metadataIndexWriterService.writeMetadata(metadata);
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot refresh metadata index while loading table.", e);
        }
    }

    private void notifyProgress(BiConsumer<Integer, Integer> progressConsumer, int processedFiles, int totalFiles) {
        if (progressConsumer != null) {
            progressConsumer.accept(processedFiles, totalFiles);
        }
    }
}
