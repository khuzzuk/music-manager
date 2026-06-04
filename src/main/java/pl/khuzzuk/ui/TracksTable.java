package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.Tag;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.settings.TrackColumn;
import pl.khuzzuk.settings.TrackSort;
import pl.khuzzuk.settings.TrackSortDirection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterListener;
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
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TracksTable extends JTable {
    private static final String ADD_SELECTED_TO_PLAYLIST_ACTION = "addSelectedToPlaylist";
    private static final String EDIT_SELECTED_METADATA_ACTION = "editSelectedMetadata";
    private static final String DELETE_SELECTED_FILES_ACTION = "deleteSelectedFiles";
    private final SettingsService settingsService;
    private final MetadataWriterService metadataWriterService;
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final TracksTableController controller;
    private final PlaylistPane playlistPane;
    private final PlayerController playerController;
    private final Consumer<List<SoundFileMetadata>> selectedTracksConsumer;
    private final TracksTableMetadataEditor metadataEditor;
    private final TracksTableFileDeleter fileDeleter;
    private final RowSorterListener sortListener = ignored -> rememberCurrentSort();
    private final List<Path> rowPaths = new ArrayList<>();
    private final List<SoundFileMetadata> rowMetadata = new ArrayList<>();
    private final Map<Tag, Integer> columnWidths = new EnumMap<>(Tag.class);
    private final TracksTableModeler modeler = new TracksTableModeler();
    private List<TrackColumn> columns;
    private List<TrackSort> currentSort;
    private RowSorter<?> observedSorter;
    private int previewRow = -1;
    private int previewModelColumn = -1;
    private boolean updatingModel;
    private boolean replacingModel;
    private boolean columnMenuShownOnPress;
    private boolean contextMenuShownOnPress;
    private int sortColumnToClear = -1;

    public TracksTable(
            Context context,
            PlaylistPane playlistPane,
            PlayerController playerController,
            Consumer<List<SoundFileMetadata>> selectedTracksConsumer) {
        this.settingsService = context.settingsService();
        this.metadataWriterService = context.metadataWriterService();
        this.metadataIndexReaderService = context.metadataIndexReaderService();
        MetadataIndexWriterService metadataIndexWriterService = context.metadataIndexWriterService();
        this.controller = new TracksTableController(context);
        this.playlistPane = playlistPane;
        this.playerController = playerController;
        this.selectedTracksConsumer = selectedTracksConsumer;
        this.metadataEditor = new TracksTableMetadataEditor(
                this,
                metadataWriterService,
                metadataIndexWriterService,
                rowPaths,
                rowMetadata,
                () -> columns,
                this::setModelValue);
        this.fileDeleter = new TracksTableFileDeleter(
                this,
                metadataIndexWriterService,
                this::setEnabled,
                this::clearRatingPreview,
                this::removeDeletedRows);
        this.currentSort = List.copyOf(context.settingsService().getSettings().trackSort());
        List<TrackColumn> configuredColumns = context.settingsService().getSettings().trackColumns();
        this.columns = configuredColumns == null || configuredColumns.isEmpty()
                ? SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS
                : List.copyOf(configuredColumns);
        rememberConfiguredColumnWidths();
        getTableHeader().addMouseListener(new HeaderMouseListener());
        setFocusTraversalKeysEnabled(false);
        modeler.modelTable(this);
        RatingMouseListener ratingMouseListener = new RatingMouseListener();
        addMouseMotionListener(ratingMouseListener);
        addMouseListener(ratingMouseListener);
        addMouseListener(new TrackMouseListener());
        registerAddSelectedToPlaylistAction();
        registerEditSelectedMetadataAction();
        registerDeleteSelectedFilesAction();
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

        replacingModel = true;
        try {
            setModel(model);
            applyColumnWidths();
            applyCurrentSort();
            installSortListener();
        } finally {
            replacingModel = false;
        }
        repaint();
    }

    private void refreshLoadedRows() {
        DefaultTableModel model = createTableModel();

        for (SoundFileMetadata metadata : rowMetadata) {
            model.addRow(columns.stream()
                    .map(column -> column.tag().getValue(metadata))
                    .toArray());
        }

        replacingModel = true;
        try {
            setModel(model);
            applyColumnWidths();
            applyCurrentSort();
            installSortListener();
        } finally {
            replacingModel = false;
        }
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
            public Class<?> getColumnClass(int column) {
                if (column < 0 || column >= columns.size()) {
                    return Object.class;
                }

                return columns.get(column).tag().isNumeric() ? Integer.class : Object.class;
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

                super.setValueAt(metadataEditor.commitFieldEdit(row, tag, value), row, column);
            }
        };
    }

    private void registerAddSelectedToPlaylistAction() {
        getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
                ADD_SELECTED_TO_PLAYLIST_ACTION);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
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

    private void registerDeleteSelectedFilesAction() {
        KeyStroke deleteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK);
        getInputMap(WHEN_FOCUSED).put(deleteKeyStroke, DELETE_SELECTED_FILES_ACTION);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(deleteKeyStroke, DELETE_SELECTED_FILES_ACTION);
        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSelectedFiles();
            }
        };
        getActionMap().put(DELETE_SELECTED_FILES_ACTION, action);
    }

    private void addSelectedToPlaylist() {
        List<SoundFileMetadata> selectedTracks = new ArrayList<>();
        for (int modelRow : selectedModelRows()) {
            selectedTracks.add(rowMetadata.get(modelRow));
        }

        if (!selectedTracks.isEmpty()) {
            selectedTracksConsumer.accept(selectedTracks);
        }
    }

    private void setSelectedTitlesToFileNames() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }

        for (int modelRow : selectedModelRows()) {
            SoundFileMetadata metadata = rowMetadata.get(modelRow);
            if (metadata.fileName() == null || metadata.fileName().isBlank()) {
                continue;
            }

            if (!metadataEditor.commitMetadataEdit(modelRow, Tag.TITLE, metadata.fileName())) {
                return;
            }
        }
    }

    private void editSelectedMetadata() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }

        List<Integer> modelRows = new ArrayList<>();
        List<SoundFileMetadata> selectedMetadata = new ArrayList<>();
        for (int modelRow : selectedModelRows()) {
            modelRows.add(modelRow);
            selectedMetadata.add(rowMetadata.get(modelRow));
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

        MetadataEditDialog.showDialog(this, selectedMetadata, writableTags, metadataIndexReaderService)
                .ifPresent(values -> metadataEditor.commitMetadataEdits(modelRows, values));
    }

    private void deleteSelectedFiles() {
        if (isEditing() && getCellEditor() != null) {
            getCellEditor().stopCellEditing();
        }

        List<Path> selectedPaths = selectedPaths();
        if (selectedPaths.isEmpty()) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
                this,
                "Usunac zaznaczone pliki? Tej operacji nie mozna cofnac.",
                "Potwierdz usuniecie",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        playerController.stopIfActiveSoundFile(selectedPaths);
        playlistPane.removeSoundFiles(selectedPaths);
        fileDeleter.deleteFilesInBackground(selectedPaths);
    }

    private List<Path> selectedPaths() {
        Set<Path> selectedPaths = new LinkedHashSet<>();
        for (int selectedRow : getSelectedRows()) {
            int modelRow = convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < rowPaths.size()) {
                selectedPaths.add(rowPaths.get(modelRow).toAbsolutePath().normalize());
            }
        }
        return List.copyOf(selectedPaths);
    }

    private List<Integer> selectedModelRows() {
        List<Integer> modelRows = new ArrayList<>();
        for (int selectedRow : getSelectedRows()) {
            int modelRow = convertRowIndexToModel(selectedRow);
            if (modelRow >= 0 && modelRow < rowMetadata.size()) {
                modelRows.add(modelRow);
            }
        }
        return List.copyOf(modelRows);
    }

    private void removeDeletedRows(List<Path> deletedPaths) {
        Set<Path> deletedPathSet = new LinkedHashSet<>(deletedPaths);
        DefaultTableModel model = (DefaultTableModel) getModel();
        for (int row = rowPaths.size() - 1; row >= 0; row--) {
            if (!deletedPathSet.contains(rowPaths.get(row).toAbsolutePath().normalize())) {
                continue;
            }

            rowPaths.remove(row);
            rowMetadata.remove(row);
            model.removeRow(row);
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

    List<TrackSort> getCurrentSort() {
        return List.copyOf(currentSort);
    }

    void selectPath(Path path) {
        if (path == null) {
            return;
        }

        Path normalizedPath = path.toAbsolutePath().normalize();
        for (int modelRow = 0; modelRow < rowPaths.size(); modelRow++) {
            if (!normalizedPath.equals(rowPaths.get(modelRow))) {
                continue;
            }

            int viewRow = convertRowIndexToView(modelRow);
            if (viewRow < 0) {
                return;
            }

            getSelectionModel().setSelectionInterval(viewRow, viewRow);
            scrollRectToVisible(getCellRect(viewRow, 0, true));
            requestFocusInWindow();
            return;
        }
    }

    private void applyCurrentSort() {
        RowSorter<?> sorter = getRowSorter();
        if (sorter == null) {
            return;
        }

        sorter.setSortKeys(currentSort.stream()
                .map(this::toSortKey)
                .filter(Objects::nonNull)
                .toList());
    }

    private RowSorter.SortKey toSortKey(TrackSort sort) {
        int columnIndex = columnIndex(sort.tag());
        if (columnIndex < 0) {
            return null;
        }

        return new RowSorter.SortKey(columnIndex, toSortOrder(sort.direction()));
    }

    private int columnIndex(Tag tag) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).tag() == tag) {
                return i;
            }
        }
        return -1;
    }

    private SortOrder toSortOrder(TrackSortDirection direction) {
        return direction == TrackSortDirection.DESCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING;
    }

    private void installSortListener() {
        RowSorter<?> sorter = getRowSorter();
        if (observedSorter == sorter) {
            return;
        }
        if (observedSorter != null) {
            observedSorter.removeRowSorterListener(sortListener);
        }
        observedSorter = sorter;
        if (observedSorter != null) {
            observedSorter.addRowSorterListener(sortListener);
        }
    }

    private void rememberCurrentSort() {
        if (replacingModel) {
            return;
        }

        RowSorter<?> sorter = getRowSorter();
        if (sorter == null) {
            currentSort = List.of();
            return;
        }

        currentSort = sorter.getSortKeys().stream()
                .filter(key -> key.getSortOrder() == SortOrder.ASCENDING || key.getSortOrder() == SortOrder.DESCENDING)
                .map(this::toTrackSort)
                .filter(Objects::nonNull)
                .toList();
    }

    private TrackSort toTrackSort(RowSorter.SortKey key) {
        int column = key.getColumn();
        if (column < 0 || column >= columns.size()) {
            return null;
        }

        return new TrackSort(columns.get(column).tag(), toTrackSortDirection(key.getSortOrder()));
    }

    private TrackSortDirection toTrackSortDirection(SortOrder sortOrder) {
        return sortOrder == SortOrder.DESCENDING
                ? TrackSortDirection.DESCENDING
                : TrackSortDirection.ASCENDING;
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
        metadataEditor.commitRating(
                ratingCell.row(),
                ratingCell.modelColumn(),
                ratingCell.rating(),
                this::clearRatingPreview);
    }

    private void setModelValue(int row, int column, Object value) {
        updatingModel = true;
        try {
            getModel().setValueAt(value, row, column);
        } finally {
            updatingModel = false;
        }
    }

    private record RatingCell(int row, int modelColumn, int rating) {
    }

    private class HeaderMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            columnMenuShownOnPress = showColumnMenu(event);
            sortColumnToClear = sortColumnToClear(event);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (columnMenuShownOnPress) {
                columnMenuShownOnPress = false;
                return;
            }
            if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
                showColumnMenu(event);
            } else {
                saveColumns();
            }
        }

        @Override
        public void mouseClicked(MouseEvent event) {
            clearSortOnThirdClick(event);
        }
    }

    private boolean showColumnMenu(MouseEvent event) {
        if (!event.isPopupTrigger() && !SwingUtilities.isRightMouseButton(event)) {
            return false;
        }

        JPopupMenu menu = new JPopupMenu();
        modeler.modelColumnMenu(menu);
        for (Tag tag : Tag.values()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(tag.label(), isColumnVisible(tag));
            modeler.modelColumnMenuItem(item);
            item.setEnabled(!item.isSelected() || columns.size() > 1);
            item.addActionListener(ignored -> setColumnVisible(tag, item.isSelected()));
            menu.add(item);
        }
        menu.show(event.getComponent(), event.getX(), event.getY());
        return true;
    }

    private void showContextMenu(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        if (row < 0) {
            return;
        }

        if (!isRowSelected(row)) {
            setRowSelectionInterval(row, row);
        }

        TracksTableContextMenu menu = new TracksTableContextMenu(
                modeler,
                this::addSelectedToPlaylist,
                this::setSelectedTitlesToFileNames,
                this::deleteSelectedFiles,
                this::editSelectedMetadata);
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    private class TrackMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            contextMenuShownOnPress = showContextMenuIfNeeded(event);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            if (contextMenuShownOnPress) {
                contextMenuShownOnPress = false;
                return;
            }

            showContextMenuIfNeeded(event);
        }

        private boolean showContextMenuIfNeeded(MouseEvent event) {
            if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
                showContextMenu(event);
                return true;
            }
            return false;
        }
    }

    private int sortColumnToClear(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) {
            return -1;
        }

        int viewColumn = getTableHeader().columnAtPoint(event.getPoint());
        if (viewColumn < 0) {
            return -1;
        }

        int modelColumn = convertColumnIndexToModel(viewColumn);
        RowSorter<?> sorter = getRowSorter();
        if (sorter == null || sorter.getSortKeys().isEmpty()) {
            return -1;
        }

        RowSorter.SortKey primarySort = sorter.getSortKeys().getFirst();
        return primarySort.getColumn() == modelColumn && primarySort.getSortOrder() == SortOrder.DESCENDING
                ? modelColumn
                : -1;
    }

    private void clearSortOnThirdClick(MouseEvent event) {
        if (sortColumnToClear < 0 || !SwingUtilities.isLeftMouseButton(event)) {
            sortColumnToClear = -1;
            return;
        }

        int viewColumn = getTableHeader().columnAtPoint(event.getPoint());
        if (viewColumn >= 0 && convertColumnIndexToModel(viewColumn) == sortColumnToClear) {
            clearSortColumn(sortColumnToClear);
        }
        sortColumnToClear = -1;
    }

    private void clearSortColumn(int modelColumn) {
        RowSorter<?> sorter = getRowSorter();
        if (sorter == null) {
            return;
        }

        sorter.setSortKeys(sorter.getSortKeys().stream()
                .filter(sortKey -> sortKey.getColumn() != modelColumn)
                .toList());
    }

    private boolean isColumnVisible(Tag tag) {
        return columns.stream().anyMatch(column -> column.tag() == tag);
    }

    private void setColumnVisible(Tag tag, boolean visible) {
        rememberCurrentColumnWidths();

        List<TrackColumn> updatedColumns = new ArrayList<>(columns);
        if (visible) {
            if (!isColumnVisible(tag)) {
                updatedColumns.add(new TrackColumn(tag, widthFor(tag)));
            }
        } else {
            updatedColumns.removeIf(column -> column.tag() == tag);
        }

        if (updatedColumns.isEmpty()) {
            return;
        }

        columns = List.copyOf(updatedColumns);
        currentSort = currentSort.stream()
                .filter(sort -> isColumnVisible(sort.tag()))
                .toList();
        refreshLoadedRows();
        saveColumns();
    }

    private void saveColumns() {
        List<TrackColumn> currentColumns = new ArrayList<>();
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            Object identifier = columnModel.getColumn(i).getIdentifier();
            currentColumns.add(new TrackColumn((Tag) identifier, columnModel.getColumn(i).getWidth()));
        }
        columns = List.copyOf(currentColumns);
        currentColumns.forEach(column -> columnWidths.put(column.tag(), column.width()));

        Settings settings = settingsService.getSettings();
        Settings newSettings = new Settings(
                settings.windowX(),
                settings.windowY(),
                settings.windowWidth(),
                settings.windowHeight(),
                settings.maximizedWindow(),
                settings.lastTreePosition(),
                settings.lastPlaylist(),
                settings.lastPlaylistPosition(),
                settings.indexedPaths(),
                settings.lastChoosenPath(),
                currentColumns,
                currentSort,
                settings.lastTracksFilterTag());
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

    private void rememberConfiguredColumnWidths() {
        for (TrackColumn column : SettingsToPropertiesMapper.DEFAULT_TRACK_COLUMNS) {
            columnWidths.put(column.tag(), column.width());
        }
        for (TrackColumn column : columns) {
            columnWidths.put(column.tag(), column.width());
        }
    }

    private void rememberCurrentColumnWidths() {
        TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            Object identifier = columnModel.getColumn(i).getIdentifier();
            if (identifier instanceof Tag tag) {
                columnWidths.put(tag, columnModel.getColumn(i).getWidth());
            }
        }
    }

    private int widthFor(Tag tag) {
        Integer width = columnWidths.get(tag);
        return width == null ? 120 : width;
    }
}
