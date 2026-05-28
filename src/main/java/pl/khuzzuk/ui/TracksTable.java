package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.Tag;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.settings.TrackColumn;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TracksTable extends JTable {
    private final SettingsService settingsService;
    private final MetadataReaderService metadataReaderService;
    private List<TrackColumn> columns;

    public TracksTable(SettingsService settingsService, MetadataReaderService metadataReaderService) {
        this.settingsService = settingsService;
        this.metadataReaderService = metadataReaderService;
        List<TrackColumn> configuredColumns = settingsService.getSettings().trackColumns();
        this.columns = configuredColumns == null || configuredColumns.isEmpty()
                ? SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS
                : List.copyOf(configuredColumns);
        getTableHeader().addMouseListener(new SaveColumnsListener());
        showMappedFiles(List.of());
    }

    public void showMappedFiles(List<IndexItem> files) {
        DefaultTableModel model = new DefaultTableModel(columns.stream()
                .map(column1 -> column1.tag().label())
                .toArray(String[]::new), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (IndexItem file : files) {
            SoundFileMetadata metadata;
            if (file instanceof SoundFileIndexItem soundFileIndexItem) {
                metadata = readMetadata(soundFileIndexItem);
            } else {
                metadata = SoundFileMetadata.empty(file.getPath());
            }

            model.addRow(columns.stream()
                    .map(column -> column.tag().getValue(metadata))
                    .toArray());
        }
        setModel(model);
        applyColumnWidths();
    }

    private SoundFileMetadata readMetadata(SoundFileIndexItem soundFileIndexItem) {
        try {
            return metadataReaderService.readMetadata(soundFileIndexItem.getPath());
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
