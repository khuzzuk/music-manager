package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.SoundFileMetadataUpdateMapper;
import pl.khuzzuk.metadata.Tag;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.settings.TrackColumn;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TracksTable extends JTable {
    private static final String ADD_SELECTED_TO_PLAYLIST_ACTION = "addSelectedToPlaylist";
    private static final String EDIT_SELECTED_METADATA_ACTION = "editSelectedMetadata";
    private final SettingsService settingsService;
    private final MetadataWriterService metadataWriterService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final TracksTableController controller;
    private final Consumer<List<SoundFileMetadata>> selectedTracksConsumer;
    private final SoundFileMetadataUpdateMapper soundFileMetadataUpdateMapper = new SoundFileMetadataUpdateMapper();
    private final List<Path> rowPaths = new ArrayList<>();
    private final List<SoundFileMetadata> rowMetadata = new ArrayList<>();
    private List<TrackColumn> columns;
    private int previewRow = -1;
    private int previewModelColumn = -1;
    private boolean updatingModel;

    public TracksTable(Context context, Consumer<List<SoundFileMetadata>> selectedTracksConsumer) {
        this.settingsService = context.settingsService();
        this.metadataWriterService = context.metadataWriterService();
        this.metadataIndexWriterService = context.metadataIndexWriterService();
        this.controller = new TracksTableController(context);
        this.selectedTracksConsumer = selectedTracksConsumer;
        List<TrackColumn> configuredColumns = context.settingsService().getSettings().trackColumns();
        this.columns = configuredColumns == null || configuredColumns.isEmpty()
                ? SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS
                : List.copyOf(configuredColumns);
        getTableHeader().addMouseListener(new SaveColumnsListener());
        RatingMouseListener ratingMouseListener = new RatingMouseListener();
        addMouseMotionListener(ratingMouseListener);
        addMouseListener(ratingMouseListener);
        registerAddSelectedToPlaylistAction();
        registerEditSelectedMetadataAction();
        showMappedFiles(List.of());
    }

    public void showMappedFiles(List<IndexItem> files) {
        showMappedFiles(files, null);
    }

    public void showMappedFiles(List<IndexItem> files, Runnable loadedCallback) {
        showMappedFiles(files, loadedCallback, null, null);
    }

    public void showMappedFiles(
            List<IndexItem> files,
            Runnable loadedCallback,
            BiConsumer<Integer, Integer> progressConsumer,
            Runnable finishedProgressCallback) {
        clearRatingPreview();

        if (files == null || files.isEmpty()) {
            notifyProgressFinished(finishedProgressCallback);
            showLoadedFiles(List.of());
            notifyLoaded(loadedCallback);
            return;
        }

        controller.loadFiles(
                files,
                progressConsumer,
                loadedTracks -> {
                    showLoadedFiles(loadedTracks);
                    notifyProgressFinished(finishedProgressCallback);
                    notifyLoaded(loadedCallback);
                },
                ignored -> JOptionPane.showMessageDialog(
                        TracksTable.this,
                        "Nie udalo sie wczytac metadanych.",
                        "Blad odczytu",
                        JOptionPane.ERROR_MESSAGE));
    }

    private void notifyProgressFinished(Runnable finishedProgressCallback) {
        if (finishedProgressCallback != null) {
            finishedProgressCallback.run();
        }
    }

    private void notifyLoaded(Runnable loadedCallback) {
        if (loadedCallback != null) {
            loadedCallback.run();
        }
    }

    private void showLoadedFiles(List<SoundFileMetadata> loadedTracks) {
        rowPaths.clear();
        rowMetadata.clear();
        DefaultTableModel model = createTableModel();

        for (SoundFileMetadata loadedTrack : loadedTracks) {
            rowPaths.add(loadedTrack.path());
            rowMetadata.add(loadedTrack);
            model.addRow(columns.stream()
                    .map(column -> column.tag().getValue(loadedTrack))
                    .toArray());
        }

        setModel(model);
        applyColumnWidths();
        repaint();
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(columns.stream()
                .map(column -> column.tag().label())
                .toArray(String[]::new), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (row < 0 || row >= rowMetadata.size()) {
                    return false;
                }

                Tag tag = columns.get(column).tag();
                return tag != Tag.RATING && metadataWriterService.canWrite(tag);
            }

            @Override
            public void setValueAt(Object value, int row, int column) {
                if (updatingModel) {
                    super.setValueAt(value, row, column);
                    return;
                }

                Tag tag = columns.get(column).tag();
                if (!isCellEditable(row, column)) {
                    super.setValueAt(value, row, column);
                    return;
                }

                super.setValueAt(commitFieldEdit(row, tag, value), row, column);
            }
        };
    }

    private void registerAddSelectedToPlaylistAction() {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                ADD_SELECTED_TO_PLAYLIST_ACTION);
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                addSelectedToPlaylist();
            }
        };
        getActionMap().put(ADD_SELECTED_TO_PLAYLIST_ACTION, action);
    }

    private void registerEditSelectedMetadataAction() {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
                EDIT_SELECTED_METADATA_ACTION);
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editSelectedMetadata();
            }
        };
        getActionMap().put(EDIT_SELECTED_METADATA_ACTION, action);
    }

    private void addSelectedToPlaylist() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        List<SoundFileMetadata> selectedTracks = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < rowMetadata.size()) {
                selectedTracks.add(rowMetadata.get(modelRow));
            }
        }

        if (!selectedTracks.isEmpty()) {
            selectedTracksConsumer.accept(selectedTracks);
        }
    }

    private void editSelectedMetadata() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }

        int[] selectedRows = getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        List<Integer> modelRows = new ArrayList<>();
        List<SoundFileMetadata> selectedMetadata = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < rowMetadata.size()) {
                modelRows.add(modelRow);
                selectedMetadata.add(rowMetadata.get(modelRow));
            }
        }

        if (modelRows.isEmpty()) {
            return;
        }

        List<Tag> writableTags = new ArrayList<>();
        for (Tag tag : Tag.values()) {
            if (metadataWriterService.canWrite(tag)) {
                writableTags.add(tag);
            }
        }

        MetadataEditDialog.showDialog(this, selectedMetadata, writableTags)
                .ifPresent(values -> commitMetadataEdits(modelRows, values));
    }

    private void commitMetadataEdits(List<Integer> rows, Map<Tag, Object> values) {
        for (int row : rows) {
            commitMetadataEdits(row, values);
        }
    }

    private void commitMetadataEdits(int row, Map<Tag, Object> values) {
        for (Map.Entry<Tag, Object> entry : values.entrySet()) {
            if (!commitMetadataEdit(row, entry.getKey(), entry.getValue())) {
                return;
            }
        }
    }

    private boolean commitMetadataEdit(int row, Tag tag, Object value) {
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
                    this,
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
        int column = -1;
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).tag() == tag) {
                column = i;
                break;
            }
        }
        if (column < 0 || column >= getModel().getColumnCount()) {
            return;
        }

        setModelValue(row, column, value);
    }

    private void applyColumnWidths() {
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columns.size() && i < columnModel.getColumnCount(); i++) {
            TrackColumn column = columns.get(i);
            int width = column.width();
            columnModel.getColumn(i).setIdentifier(column.tag());
            columnModel.getColumn(i).setPreferredWidth(width);
            columnModel.getColumn(i).setWidth(width);
            if (column.tag() == Tag.RATING) {
                columnModel.getColumn(i).setCellRenderer(new RatingCellRenderer());
            }
        }
    }

    private class RatingMouseListener extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent event) {
            showRatingPreview(event);
        }

        @Override
        public void mouseExited(MouseEvent event) {
            clearRatingPreview();
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (!SwingUtilities.isLeftMouseButton(event)) {
                return;
            }

            RatingCell ratingCell = getRatingCell(event);
            if (ratingCell == null) {
                return;
            }

            commitRating(ratingCell);
        }
    }

    private void showRatingPreview(MouseEvent event) {
        RatingCell ratingCell = getRatingCell(event);
        if (ratingCell == null) {
            clearRatingPreview();
            return;
        }

        if (previewRow != ratingCell.row() || previewModelColumn != ratingCell.modelColumn()) {
            clearRatingPreview();
        }

        previewRow = ratingCell.row();
        previewModelColumn = ratingCell.modelColumn();
        setModelValue(ratingCell.row(), ratingCell.modelColumn(), ratingCell.rating());
    }

    private void clearRatingPreview() {
        if (previewRow < 0 || previewModelColumn < 0 || previewRow >= rowMetadata.size()) {
            previewRow = -1;
            previewModelColumn = -1;
            return;
        }

        setModelValue(previewRow, previewModelColumn, rowMetadata.get(previewRow).rating());
        previewRow = -1;
        previewModelColumn = -1;
    }

    private RatingCell getRatingCell(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        int viewColumn = columnAtPoint(event.getPoint());
        if (row < 0 || viewColumn < 0) {
            return null;
        }

        int modelRow = convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= rowMetadata.size()) {
            return null;
        }

        Object identifier = getColumnModel().getColumn(viewColumn).getIdentifier();
        if (identifier != Tag.RATING) {
            return null;
        }

        int modelColumn = convertColumnIndexToModel(viewColumn);
        Rectangle cellRect = getCellRect(row, viewColumn, false);
        int rating = getRatingFromMousePosition(event.getX() - cellRect.x, cellRect.width);
        return new RatingCell(modelRow, modelColumn, rating);
    }

    private int getRatingFromMousePosition(int x, int width) {
        if (width <= 0 || x <= 0) {
            return 0;
        }

        int rating = (int) Math.ceil(x * 10.0 / width);
        return Math.clamp(rating, 0, 10);
    }

    private void commitRating(RatingCell ratingCell) {
        Path path = rowPaths.get(ratingCell.row());
        try {
            metadataWriterService.writeRating(path, ratingCell.rating());
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nie udalo sie zapisac ratingu w pliku.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
            clearRatingPreview();
            return;
        }

        SoundFileMetadata metadata = rowMetadata.get(ratingCell.row());
        soundFileMetadataUpdateMapper.setValue(metadata, Tag.RATING, ratingCell.rating());
        setModelValue(ratingCell.row(), ratingCell.modelColumn(), ratingCell.rating());
        writeMetadataIndex(metadata);
    }

    private Object commitFieldEdit(int row, Tag tag, Object value) {
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
                    this,
                    "Nie udalo sie zapisac metadanych w pliku.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
            return currentValue;
        }

        soundFileMetadataUpdateMapper.setValue(metadata, tag, newValue);
        writeMetadataIndex(metadata);
        return newValue;
    }

    private void setModelValue(int row, int column, Object value) {
        updatingModel = true;
        try {
            getModel().setValueAt(value, row, column);
        } finally {
            updatingModel = false;
        }
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
                    this,
                    "Nie udalo sie zaktualizowac indeksu metadanych.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private record RatingCell(int row, int modelColumn, int rating) {
    }

    private class SaveColumnsListener extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent event) {
            saveColumns();
        }
    }

    private void saveColumns() {
        List<TrackColumn> currentColumns = new ArrayList<>();
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            Object identifier = columnModel.getColumn(i).getIdentifier();
            currentColumns.add(new TrackColumn((Tag) identifier, columnModel.getColumn(i).getWidth()));
        }
        columns = List.copyOf(currentColumns);

        Settings settings = settingsService.getSettings();
        Settings newSettings = new Settings(
                settings.windowX(),
                settings.windowY(),
                settings.windowWidth(),
                settings.windowHeight(),
                settings.maximizedWindow(),
                settings.lastTreePosition(),
                settings.lastPlaylist(),
                settings.indexedPaths(),
                settings.lastChoosenPath(),
                currentColumns);
        try {
            settingsService.saveSettings(newSettings);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nie udalo sie zapisac ustawien kolumn.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
