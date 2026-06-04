package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.SoundFileMetadataUpdateMapper;
import pl.khuzzuk.metadata.Tag;
import pl.khuzzuk.settings.TrackColumn;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

class TracksTableMetadataEditor {
    private final Component parent;
    private final MetadataWriterService metadataWriterService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final List<Path> rowPaths;
    private final List<SoundFileMetadata> rowMetadata;
    private final Supplier<List<TrackColumn>> columnsSupplier;
    private final TableValueUpdater tableValueUpdater;
    private final SoundFileMetadataUpdateMapper soundFileMetadataUpdateMapper = new SoundFileMetadataUpdateMapper();

    TracksTableMetadataEditor(
            Component parent,
            MetadataWriterService metadataWriterService,
            MetadataIndexWriterService metadataIndexWriterService,
            List<Path> rowPaths,
            List<SoundFileMetadata> rowMetadata,
            Supplier<List<TrackColumn>> columnsSupplier,
            TableValueUpdater tableValueUpdater) {
        this.parent = parent;
        this.metadataWriterService = metadataWriterService;
        this.metadataIndexWriterService = metadataIndexWriterService;
        this.rowPaths = rowPaths;
        this.rowMetadata = rowMetadata;
        this.columnsSupplier = columnsSupplier;
        this.tableValueUpdater = tableValueUpdater;
    }

    void commitMetadataEdits(List<Integer> rows, Map<Tag, Object> values) {
        for (int row : rows) {
            commitMetadataEdits(row, values);
        }
    }

    boolean commitMetadataEdit(int row, Tag tag, Object value) {
        SoundFileMetadata metadata = rowMetadata.get(row);
        Object currentValue = tag.getValue(metadata);
        if (!hasChanged(tag, currentValue, value)) {
            return true;
        }

        Path path = rowPaths.get(row);
        try {
            if (tag == Tag.RATING) {
                metadataWriterService.writeRating(path, toRating(value));
            } else {
                metadataWriterService.writeTag(path, tag, normalizeValue(value));
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie zapisac pola: " + tag.label() + ".",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        soundFileMetadataUpdateMapper.setValue(metadata, tag, value);
        updateVisibleCell(row, tag, tag.getValue(metadata));
        writeMetadataIndex(metadata);
        return true;
    }

    Object commitFieldEdit(int row, Tag tag, Object value) {
        SoundFileMetadata metadata = rowMetadata.get(row);
        Object currentValue = tag.getValue(metadata);
        String newValue = normalizeValue(value);
        String oldValue = normalizeValue(currentValue);
        if (Objects.equals(oldValue, newValue)) {
            return currentValue;
        }

        Path path = rowPaths.get(row);
        try {
            metadataWriterService.writeTag(path, tag, newValue);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie zapisac metadanych w pliku.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
            return currentValue;
        }

        soundFileMetadataUpdateMapper.setValue(metadata, tag, newValue);
        writeMetadataIndex(metadata);
        return newValue;
    }

    void commitRating(int row, int modelColumn, int rating, Runnable failedCommitCallback) {
        Path path = rowPaths.get(row);
        try {
            metadataWriterService.writeRating(path, rating);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie zapisac ratingu w pliku.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
            failedCommitCallback.run();
            return;
        }

        SoundFileMetadata metadata = rowMetadata.get(row);
        soundFileMetadataUpdateMapper.setValue(metadata, Tag.RATING, rating);
        tableValueUpdater.update(row, modelColumn, rating);
        writeMetadataIndex(metadata);
    }

    private void commitMetadataEdits(int row, Map<Tag, Object> values) {
        for (Map.Entry<Tag, Object> entry : values.entrySet()) {
            if (!commitMetadataEdit(row, entry.getKey(), entry.getValue())) {
                return;
            }
        }
    }

    private boolean hasChanged(Tag tag, Object currentValue, Object newValue) {
        if (tag == Tag.RATING) {
            return toRating(currentValue) != toRating(newValue);
        }

        String oldValue = normalizeValue(currentValue);
        String value = normalizeValue(newValue);
        return !Objects.equals(oldValue, value);
    }

    private int toRating(Object value) {
        if (value instanceof Number number) {
            return Math.clamp(number.intValue(), 0, 10);
        }
        if (value == null || value.toString().isBlank()) {
            return 0;
        }

        return Math.clamp(Integer.parseInt(value.toString().trim()), 0, 10);
    }

    private void updateVisibleCell(int row, Tag tag, Object value) {
        List<TrackColumn> columns = columnsSupplier.get();
        int column = -1;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).tag() == tag) {
                column = i;
                break;
            }
        }
        if (column < 0) {
            return;
        }

        tableValueUpdater.update(row, column, value);
    }

    private String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private void writeMetadataIndex(SoundFileMetadata metadata) {
        try {
            metadataIndexWriterService.writeMetadata(metadata);
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie zaktualizowac indeksu metadanych.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @FunctionalInterface
    interface TableValueUpdater {
        void update(int row, int column, Object value);
    }
}
