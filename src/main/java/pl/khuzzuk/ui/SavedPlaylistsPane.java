package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.player.SoundFile;
import pl.khuzzuk.playlist.SavedPlaylist;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;

class SavedPlaylistsPane extends JPanel {
    private static final String DELETE_SELECTED_ACTION = "deleteSelectedSavedPlaylists";
    private static final String LOAD_SELECTED_ACTION = "loadSelectedSavedPlaylist";

    private final Context context;
    private final PlaylistPane playlistPane;
    private final DefaultListModel<SavedPlaylist> playlistsModel = new DefaultListModel<>();
    private final JList<SavedPlaylist> playlists = new JList<>(playlistsModel);

    SavedPlaylistsPane(Context context, PlaylistPane playlistPane) {
        super(new BorderLayout(0, 6));
        this.context = context;
        this.playlistPane = playlistPane;
        SavedPlaylistsPaneModeler modeler = new SavedPlaylistsPaneModeler();
        modeler.modelPane(this);

        JLabel title = new JLabel("Playlisty");
        modeler.modelTitle(title);
        add(title, BorderLayout.NORTH);

        playlists.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        playlists.setCellRenderer(new SavedPlaylistCellRenderer());
        modeler.modelList(playlists);
        registerListActions();

        JScrollPane scrollPane = new JScrollPane(playlists);
        modeler.modelScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        modeler.modelButtonsPanel(buttons);
        JButton saveButton = new JButton("Zapisz");
        JButton loadButton = new JButton("Wczytaj");
        modeler.modelSaveButton(saveButton);
        modeler.modelLoadButton(loadButton);
        saveButton.addActionListener(ignored -> saveCurrentPlaylist());
        loadButton.addActionListener(ignored -> loadSelectedPlaylist());
        buttons.add(saveButton);
        buttons.add(loadButton);
        add(buttons, BorderLayout.SOUTH);

        refreshPlaylists();
    }

    private void registerListActions() {
        playlists.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
                DELETE_SELECTED_ACTION);
        playlists.getActionMap().put(DELETE_SELECTED_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteSelectedPlaylists();
            }
        });
        playlists.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                LOAD_SELECTED_ACTION);
        playlists.getActionMap().put(LOAD_SELECTED_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                loadSelectedPlaylist();
            }
        });
        playlists.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    loadSelectedPlaylist();
                }
            }
        });
    }

    private void refreshPlaylists() {
        try {
            playlistsModel.clear();
            for (SavedPlaylist playlist : context.savedPlaylistService().listPlaylists()) {
                playlistsModel.addElement(playlist);
            }
        } catch (IOException | SecurityException e) {
            reportError("Cannot list saved playlists.", e, "Nie udalo sie wczytac zapisanych playlist.");
        }
    }

    private void saveCurrentPlaylist() {
        List<SoundFile> soundFiles = playlistPane.getSoundFiles();
        if (soundFiles.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Biezaca playlista jest pusta.",
                    "Zapis playlisty",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String name = JOptionPane.showInputDialog(this, "Nazwa playlisty:", "Zapis playlisty", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }

        try {
            SavedPlaylist savedPlaylist = context.savedPlaylistService().savePlaylist(name, soundFiles);
            refreshPlaylists();
            playlists.setSelectedValue(savedPlaylist, true);
        } catch (IOException | SecurityException e) {
            reportError("Cannot save playlist.", e, "Nie udalo sie zapisac playlisty.");
        }
    }

    private void loadSelectedPlaylist() {
        SavedPlaylist selectedPlaylist = playlists.getSelectedValue();
        if (selectedPlaylist == null) {
            return;
        }

        try {
            playlistPane.replaceTracks(context.savedPlaylistService().readPlaylist(selectedPlaylist));
        } catch (IOException | SecurityException e) {
            reportError("Cannot load playlist.", e, "Nie udalo sie wczytac playlisty.");
        }
    }

    private void deleteSelectedPlaylists() {
        List<SavedPlaylist> selectedPlaylists = playlists.getSelectedValuesList();
        if (selectedPlaylists.isEmpty()) {
            return;
        }

        int decision = JOptionPane.showConfirmDialog(
                this,
                "Usunac zaznaczone playlisty?",
                "Usuwanie playlist",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            for (SavedPlaylist playlist : selectedPlaylists) {
                context.savedPlaylistService().deletePlaylist(playlist);
            }
            refreshPlaylists();
        } catch (IOException | SecurityException e) {
            reportError("Cannot delete playlist.", e, "Nie udalo sie usunac playlisty.");
        }
    }

    private void reportError(String logMessage, Exception exception, String userMessage) {
        ErrorReporter.log(logMessage, exception);
        JOptionPane.showMessageDialog(this, userMessage, "Blad playlist", JOptionPane.ERROR_MESSAGE);
    }

    private static class SavedPlaylistCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focused) {
            SavedPlaylist playlist = (SavedPlaylist) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, playlist.name(), index, selected, focused);
            label.setBorder(UiTheme.empty(5, 8, 5, 8));
            return label;
        }
    }
}
