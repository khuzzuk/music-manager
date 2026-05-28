package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.metadata.SoundFileMetadata;
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
import java.util.Locale;
import java.util.function.Function;

public class TracksTable extends JTable {
    private final SettingsService settingsService;
    private List<TrackColumn> columns;

    public TracksTable(SettingsService settingsService) {
        this.settingsService = settingsService;
        List<TrackColumn> configuredColumns = settingsService.getSettings().trackColumns();
        this.columns = configuredColumns == null || configuredColumns.isEmpty()
                ? SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS
                : List.copyOf(configuredColumns);
        getTableHeader().addMouseListener(new SaveColumnsListener());
        showMappedFiles(List.of());
    }

    public void showMappedFiles(List<IndexItem> files) {
        DefaultTableModel model = new DefaultTableModel(getColumnLabels(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        files.forEach(file -> {
            SoundFileMetadata metadata = getMetadata(file);
            model.addRow(columns.stream()
                    .map(column -> getColumnValue(column.name(), metadata))
                    .toArray());
        });
        setModel(model);
        applyColumnWidths();
    }

    private SoundFileMetadata getMetadata(IndexItem file) {
        if (file instanceof SoundFileIndexItem soundFileIndexItem) {
            return soundFileIndexItem.getMetadata();
        }

        return SoundFileMetadata.empty(file.getPath());
    }

    private String[] getColumnLabels() {
        return columns.stream()
                .map(column -> getColumnDefinition(column.name()).label())
                .toArray(String[]::new);
    }

    private Object getColumnValue(String columnName, SoundFileMetadata metadata) {
        return getColumnDefinition(columnName).valueProvider().apply(metadata);
    }

    private void applyColumnWidths() {
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columns.size() && i < columnModel.getColumnCount(); i++) {
            TrackColumn column = columns.get(i);
            int width = column.width();
            columnModel.getColumn(i).setIdentifier(column.name());
            columnModel.getColumn(i).setPreferredWidth(width);
            columnModel.getColumn(i).setWidth(width);
        }
    }

    private ColumnDefinition getColumnDefinition(String columnName) {
        return switch (columnName.toLowerCase(Locale.ROOT)) {
            case "title" -> new ColumnDefinition("Tytul", SoundFileMetadata::title);
            case "album" -> new ColumnDefinition("Album", SoundFileMetadata::album);
            case "composer" -> new ColumnDefinition("Kompozytor", SoundFileMetadata::composer);
            case "rating" -> new ColumnDefinition("Rating", SoundFileMetadata::rating);
            case "mood" -> new ColumnDefinition("Mood", SoundFileMetadata::mood);
            case "movement" -> new ColumnDefinition("Movement", SoundFileMetadata::movement);
            case "occasion" -> new ColumnDefinition("Occasion", SoundFileMetadata::occasion);
            default -> new ColumnDefinition(columnName, metadata -> null);
        };
    }

    private record ColumnDefinition(String label, Function<SoundFileMetadata, Object> valueProvider) {
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
            currentColumns.add(new TrackColumn(identifier.toString(), columnModel.getColumn(i).getWidth()));
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
