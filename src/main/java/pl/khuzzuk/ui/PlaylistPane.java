package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.player.PlaylistSoundFile;
import pl.khuzzuk.player.SoundFile;
import pl.khuzzuk.settings.Settings;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class PlaylistPane extends JScrollPane {
    private static final String REMOVE_SELECTED_ACTION = "removeSelected";
    private final PlaylistTableModel playlistModel = new PlaylistTableModel();
    private final PlaylistMapper playlistMapper = new PlaylistMapper();
    private final PlaylistPaneModeler modeler = new PlaylistPaneModeler();
    private final JTable playlist;
    private PlaylistSoundFile first;
    private PlaylistSoundFile last;
    private PlaylistSoundFile currentPlaying;
    private Consumer<Path> goToPathConsumer;
    private boolean contextMenuShownOnPress;

    public PlaylistPane(Context context) {
        super();
        modeler.modelPane(this);
        playlist = new JTable(playlistModel);
        playlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlist.setTableHeader(null);
        modeler.modelPlaylist(playlist);
        configureColumns();
        registerRemoveSelectedAction();
        playlist.addMouseListener(new PlaylistMouseListener());
        setViewportView(playlist);
        restorePlaylist(context.settingsService().getSettings());
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.clip(UiTheme.roundedShape(this));
            super.paintChildren(graphics2D);
        } finally {
            graphics2D.dispose();
        }
    }

    void setGoToPathConsumer(Consumer<Path> goToPathConsumer) {
        this.goToPathConsumer = goToPathConsumer;
    }

    public void addTracks(List<SoundFileMetadata> tracks) {
        for (SoundFileMetadata track : tracks) {
            append(new PlaylistSoundFile(new SoundFile(track.path().toString(), title(track))));
        }
    }

    List<SoundFile> getSoundFiles() {
        return soundFiles();
    }

    void replaceTracks(List<SoundFile> soundFiles) {
        clear();
        for (SoundFile soundFile : soundFiles) {
            append(new PlaylistSoundFile(soundFile));
        }
        currentPlaying = first;
        playlistModel.fireTableDataChanged();
    }

    String getPlaylistPaths() {
        return playlistMapper.toSettingsValue(soundFiles());
    }

    int getCurrentPosition() {
        int position = 0;
        PlaylistSoundFile current = first;
        while (current != null) {
            if (current == currentPlaying) {
                return position;
            }

            current = current.next();
            position++;
        }

        return -1;
    }

    private void append(PlaylistSoundFile playlistSoundFile) {
        int row = playlistModel.getRowCount();
        if (first == null) {
            first = playlistSoundFile;
        }
        if (last != null) {
            last.setNext(playlistSoundFile);
            playlistSoundFile.setPrevious(last);
        }

        last = playlistSoundFile;
        if (currentPlaying == null) {
            currentPlaying = playlistSoundFile;
        }
        playlistModel.fireTableRowsInserted(row, row);
    }

    private void restorePlaylist(Settings settings) {
        List<SoundFile> soundFiles = playlistMapper.toSoundFiles(settings.lastPlaylist());
        for (SoundFile soundFile : soundFiles) {
            append(new PlaylistSoundFile(soundFile));
        }

        currentPlaying = playlistModel.getAt(settings.lastPlaylistPosition());
        if (currentPlaying == null) {
            currentPlaying = first;
        }
        playlistModel.fireTableDataChanged();
    }

    private void clear() {
        first = null;
        last = null;
        currentPlaying = null;
    }

    private List<SoundFile> soundFiles() {
        List<SoundFile> soundFiles = new ArrayList<>();
        PlaylistSoundFile current = first;
        while (current != null) {
            soundFiles.add(current.soundFile());
            current = current.next();
        }

        return soundFiles;
    }

    public SoundFile getCurrentSoundFile() {
        return currentPlaying == null ? null : currentPlaying.soundFile();
    }

    public void removeSoundFiles(Collection<Path> paths) {
        Set<Path> removablePaths = normalizePaths(paths);
        if (removablePaths.isEmpty()) {
            return;
        }

        boolean removed = false;
        PlaylistSoundFile current = first;
        while (current != null) {
            PlaylistSoundFile next = current.next();
            Path currentPath = Path.of(current.soundFile().path()).toAbsolutePath().normalize();
            if (removablePaths.contains(currentPath)) {
                unlink(current);
                removed = true;
            }
            current = next;
        }

        if (removed) {
            playlistModel.fireTableDataChanged();
            playlist.repaint();
        }
    }

    public SoundFile moveToNextSoundFile() {
        if (currentPlaying == null || first == null) {
            return null;
        }

        currentPlaying = currentPlaying.next() == null ? first : currentPlaying.next();
        playlistModel.fireTableDataChanged();
        return currentPlaying.soundFile();
    }

    public SoundFile moveToPreviousSoundFile() {
        if (currentPlaying == null || last == null) {
            return null;
        }

        currentPlaying = currentPlaying.previous() == null ? last : currentPlaying.previous();
        playlistModel.fireTableDataChanged();
        return currentPlaying.soundFile();
    }

    private void configureColumns() {
        TableColumnModel columnModel = playlist.getColumnModel();
        columnModel.getColumn(0).setMinWidth(38);
        columnModel.getColumn(0).setMaxWidth(38);
        columnModel.getColumn(0).setPreferredWidth(38);
        columnModel.getColumn(1).setMinWidth(38);
        columnModel.getColumn(1).setMaxWidth(48);
        columnModel.getColumn(1).setPreferredWidth(38);
    }

    private void registerRemoveSelectedAction() {
        playlist.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), REMOVE_SELECTED_ACTION);
        playlist.getActionMap().put(REMOVE_SELECTED_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                removeSelected();
            }
        });
    }

    private void removeSelected() {
        int[] selectedRows = playlist.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }

        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int selectedRow = selectedRows[i];
            PlaylistSoundFile playlistSoundFile = Objects.requireNonNull(playlistModel.getAt(selectedRow));
            unlink(playlistSoundFile);
            playlistModel.fireTableRowsDeleted(selectedRow, selectedRow);
        }

        playlist.repaint();
    }

    private void unlink(PlaylistSoundFile playlistSoundFile) {
        PlaylistSoundFile previous = playlistSoundFile.previous();
        PlaylistSoundFile next = playlistSoundFile.next();

        if (playlistSoundFile == currentPlaying) {
            currentPlaying = next != null ? next : previous;
        }

        if (previous == null) {
            first = next;
        } else {
            previous.setNext(next);
        }

        if (next == null) {
            last = previous;
        } else {
            next.setPrevious(previous);
        }

        playlistSoundFile.setPrevious(null);
        playlistSoundFile.setNext(null);
    }

    private Set<Path> normalizePaths(Collection<Path> paths) {
        Set<Path> normalizedPaths = new HashSet<>();
        if (paths == null) {
            return normalizedPaths;
        }

        for (Path path : paths) {
            if (path != null) {
                normalizedPaths.add(path.toAbsolutePath().normalize());
            }
        }
        return normalizedPaths;
    }

    private String title(SoundFileMetadata track) {
        if (track.title() != null && !track.title().isBlank()) {
            return track.title();
        }
        if (track.fileName() != null && !track.fileName().isBlank()) {
            return track.fileName();
        }

        return "";
    }

    private void showContextMenu(MouseEvent event) {
        int row = playlist.rowAtPoint(event.getPoint());
        if (row < 0) {
            return;
        }

        if (!playlist.isRowSelected(row)) {
            playlist.setRowSelectionInterval(row, row);
        }

        PlaylistSoundFile playlistSoundFile = playlistModel.getAt(row);
        if (playlistSoundFile == null || goToPathConsumer == null) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        modeler.modelContextMenu(menu);
        JMenuItem goToItem = new JMenuItem("Idź do");
        modeler.modelContextMenuItem(goToItem);
        goToItem.addActionListener(ignored ->
                goToPathConsumer.accept(Path.of(playlistSoundFile.soundFile().path())));
        menu.add(goToItem);
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    private int numberOf(PlaylistSoundFile playlistSoundFile) {
        int number = 1;
        PlaylistSoundFile current = first;
        while (current != null) {
            if (current == playlistSoundFile) {
                return number;
            }

            current = current.next();
            number++;
        }

        return 0;
    }

    private class PlaylistMouseListener extends MouseAdapter {
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

    private class PlaylistTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            int rows = 0;
            PlaylistSoundFile current = first;
            while (current != null) {
                rows++;
                current = current.next();
            }

            return rows;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PlaylistSoundFile playlistSoundFile = getAt(rowIndex);
            if (playlistSoundFile == null) {
                return "";
            }

            return switch (columnIndex) {
                case 0 -> playlistSoundFile == currentPlaying ? ">" : "";
                case 1 -> numberOf(playlistSoundFile);
                case 2 -> playlistSoundFile.soundFile().title();
                default -> "";
            };
        }

        private PlaylistSoundFile getAt(int row) {
            int currentRow = 0;
            PlaylistSoundFile current = first;
            while (current != null) {
                if (currentRow == row) {
                    return current;
                }

                current = current.next();
                currentRow++;
            }

            return null;
        }
    }
}
