package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.metadata.MetadataReaderService;
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
import java.util.function.Consumer;

public class TracksTable extends JTable {
    private static final String ADD_SELECTED_TO_PLAYLIST_ACTION = "addSelectedToPlaylist";
    private final SettingsService settingsService;
    private final MetadataReaderService metadataReaderService;
    private final MetadataWriterService metadataWriterService;
    private final MetadataIndexWriterService metadataIndexWriterService;
    private final Consumer<List<SoundFileMetadata>> selectedTracksConsumer;
    private final SoundFileMetadataUpdateMapper soundFileMetadataUpdateMapper = new SoundFileMetadataUpdateMapper();
    private final List<Path> rowPaths = new ArrayList<>();
    private final List<SoundFileMetadata> rowMetadata = new ArrayList<>();
    private List<TrackColumn> columns;
    private int previewRow = -1;
    private int previewModelColumn = -1;

    public TracksTable(Context context, Consumer<List<SoundFileMetadata>> selectedTracksConsumer) {
        this.settingsService = context.settingsService();
        this.metadataReaderService = context.metadataReaderService();
        this.metadataWriterService = context.metadataWriterService();
        this.metadataIndexWriterService = context.metadataIndexWriterService();
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
        showMappedFiles(List.of());
    }

    public void showMappedFiles(List<IndexItem> files) {
        clearRatingPreview();
        rowPaths.clear();
        rowMetadata.clear();
        DefaultTableModel model = new DefaultTableModel(columns.stream()
                .map(column1 -> column1.tag().label())
                .toArray(String[]::new), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                Tag tag = columns.get(column).tag();
                return tag != Tag.RATING && metadataWriterService.canWrite(tag);
            }

            @Override
            public void setValueAt(Object value, int row, int column) {
                Tag tag = columns.get(column).tag();
                if (!isCellEditable(row, column)) {
                    super.setValueAt(value, row, column);
                    return;
                }

                super.setValueAt(commitFieldEdit(row, tag, value), row, column);
            }
        };
        for (IndexItem file : files) {
            SoundFileMetadata metadata;
            if (file instanceof SoundFileIndexItem soundFileIndexItem) {
                metadata = readMetadata(soundFileIndexItem);
            } else {
                metadata = SoundFileMetadata.empty(file.getPath());
            }

            rowPaths.add(file.getPath());
            rowMetadata.add(metadata);
            model.addRow(columns.stream()
                    .map(column -> column.tag().getValue(metadata))
                    .toArray());
        }
        setModel(model);
        applyColumnWidths();
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

    private SoundFileMetadata readMetadata(SoundFileIndexItem soundFileIndexItem) {
        try {
            SoundFileMetadata metadata = metadataReaderService.readMetadata(soundFileIndexItem.getPath());
            writeMetadataIndex(metadata, false);
            return metadata;
        } catch (IOException | SecurityException e) {
            return SoundFileMetadata.empty(soundFileIndexItem.getPath());
        }
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
        getModel().setValueAt(ratingCell.rating(), ratingCell.row(), ratingCell.modelColumn());
    }

    private void clearRatingPreview() {
        if (previewRow < 0 || previewModelColumn < 0 || previewRow >= rowMetadata.size()) {
            previewRow = -1;
            previewModelColumn = -1;
            return;
        }

        getModel().setValueAt(rowMetadata.get(previewRow).rating(), previewRow, previewModelColumn);
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
        getModel().setValueAt(ratingCell.rating(), ratingCell.row(), ratingCell.modelColumn());
        writeMetadataIndex(metadata, true);
    }

    private Object commitFieldEdit(int row, Tag tag, Object value) {
        SoundFileMetadata metadata = rowMetadata.get(row);
        Object currentValue = tag.getValue(metadata);
        String newValue = normalizeValue(value);
        String oldValue = normalizeValue(currentValue);
        if (equalsNullable(oldValue, newValue)) {
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
        writeMetadataIndex(metadata, true);
        return newValue;
    }

    private String normalizeValue(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private boolean equalsNullable(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private void writeMetadataIndex(SoundFileMetadata metadata, boolean showError) {
        try {
            metadataIndexWriterService.writeMetadata(metadata);
        } catch (IOException | SecurityException e) {
            if (!showError) {
                return;
            }

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
