package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.index.RootIndexItem;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.metadata.MetadataIndexWriterService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

class TracksTableFileDeleter {
    private final Component parent;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final IndexService indexService;
    private final Supplier<RootIndexItem> rootSupplier;
    private final Consumer<Boolean> enabledConsumer;
    private final Runnable clearPreviewAction;
    private final Consumer<List<Path>> deletedRowsConsumer;

    TracksTableFileDeleter(
            Component parent,
            MetadataIndexWriterService metadataIndexWriterService,
            IndexService indexService,
            Supplier<RootIndexItem> rootSupplier,
            Consumer<Boolean> enabledConsumer,
            Runnable clearPreviewAction,
            Consumer<List<Path>> deletedRowsConsumer) {
        this.parent = parent;
        this.metadataIndexWriterService = metadataIndexWriterService;
        this.indexService = indexService;
        this.rootSupplier = rootSupplier;
        this.enabledConsumer = enabledConsumer;
        this.clearPreviewAction = clearPreviewAction;
        this.deletedRowsConsumer = deletedRowsConsumer;
    }

    void deleteFilesInBackground(List<Path> selectedPaths) {
        enabledConsumer.accept(false);
        new SwingWorker<DeleteResult, Void>() {
            @Override
            protected DeleteResult doInBackground() {
                List<Path> deletedPaths = new ArrayList<>();
                List<Path> failedPaths = new ArrayList<>();
                for (Path path : selectedPaths) {
                    try {
                        Files.delete(path);
                        deletedPaths.add(path);
                    } catch (IOException | SecurityException e) {
                        failedPaths.add(path);
                    }
                }

                deleteMetadata(deletedPaths);
                boolean indexUpdated = removeIndexedFiles(deletedPaths);
                return new DeleteResult(deletedPaths, failedPaths, indexUpdated);
            }

            @Override
            protected void done() {
                enabledConsumer.accept(true);
                try {
                    DeleteResult result = get();
                    clearPreviewAction.run();
                    deletedRowsConsumer.accept(result.deletedPaths());
                    showIndexUpdateFailure(result);
                    showDeleteFailures(result.failedPaths());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            parent,
                            "Nie udalo sie usunac zaznaczonych plikow.",
                            "Blad usuwania",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private boolean removeIndexedFiles(List<Path> deletedPaths) {
        if (deletedPaths.isEmpty()) {
            return true;
        }

        try {
            indexService.removeIndexedFiles(rootSupplier.get(), deletedPaths);
            return true;
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot remove deleted files from directory index.", e);
            return false;
        }
    }

    private void deleteMetadata(List<Path> deletedPaths) {
        if (deletedPaths.isEmpty()) {
            return;
        }

        try {
            metadataIndexWriterService.deleteMetadata(deletedPaths);
        } catch (IOException | SecurityException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie zaktualizowac indeksu metadanych.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private void showDeleteFailures(List<Path> failedPaths) {
        if (failedPaths.isEmpty()) {
            return;
        }

        JOptionPane.showMessageDialog(
                parent,
                "Nie udalo sie usunac plikow: " + failedPaths.size() + ".",
                "Blad usuwania",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showIndexUpdateFailure(DeleteResult result) {
        if (result.indexUpdated() || result.deletedPaths().isEmpty()) {
            return;
        }

        JOptionPane.showMessageDialog(
                parent,
                "Pliki zostaly usuniete, ale nie udalo sie zaktualizowac indeksu katalogow.",
                "Blad zapisu indeksu",
                JOptionPane.ERROR_MESSAGE);
    }

    private record DeleteResult(List<Path> deletedPaths, List<Path> failedPaths, boolean indexUpdated) {
    }
}
