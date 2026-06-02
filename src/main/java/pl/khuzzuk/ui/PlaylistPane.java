package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.player.PlaylistSoundFile;
import pl.khuzzuk.player.SoundFile;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

public class PlaylistPane extends JScrollPane {
    private static final String REMOVE_SELECTED_ACTION = "removeSelected";
    private final PlaylistTableModel playlistModel = new PlaylistTableModel();
    private final JTable playlist;
    private PlaylistSoundFile first;
    private PlaylistSoundFile last;
    private PlaylistSoundFile currentPlaying;

    public PlaylistPane() {
        super();
        PlaylistPaneModeler modeler = new PlaylistPaneModeler();
        modeler.modelPane(this);
        playlist = new JTable(playlistModel);
        playlist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlist.setTableHeader(null);
        modeler.modelPlaylist(playlist);
        configureColumns();
        registerRemoveSelectedAction();
        setViewportView(playlist);
    }

    public void addTracks(List<SoundFileMetadata> tracks) {
        for (SoundFileMetadata track : tracks) {
            append(new PlaylistSoundFile(new SoundFile(track.path().toString(), title(track))));
        }
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

    public SoundFile getCurrentSoundFile() {
        return currentPlaying == null ? null : currentPlaying.soundFile();
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
        columnModel.getColumn(0).setMinWidth(24);
        columnModel.getColumn(0).setMaxWidth(24);
        columnModel.getColumn(0).setPreferredWidth(24);
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

    private String title(SoundFileMetadata track) {
        if (track.title() != null && !track.title().isBlank()) {
            return track.title();
        }
        if (track.fileName() != null && !track.fileName().isBlank()) {
            return track.fileName();
        }

        return "";
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
