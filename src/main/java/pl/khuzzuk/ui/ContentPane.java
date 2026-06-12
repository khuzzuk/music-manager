package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.index.IndexProgress;
import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.metadata.Tag;
import pl.khuzzuk.settings.TrackSort;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentPane extends JPanel {
    private static final String TRACKS_CARD = "tracks";
    private static final String PROGRESS_CARD = "progress";

    private final CardLayout tracksCardLayout = new CardLayout();
    private final JPanel tracksArea = new JPanel(tracksCardLayout);
    private final TracksTable tracksTable;
    private final ProgressPanel progressPanel = new ProgressPanel();
    private final Context context;
    private final FileTree fileTree;
    private TracksFilter.Selection filterSelection;
    private List<IndexItem> selectedTreeFiles = List.of();
    private boolean treeSelectionActive;
    private boolean indexing;

    public ContentPane(Context context, PlaylistPane playlist, PlayerController playerController) {
        super(new GridBagLayout());
        this.context = context;
        this.filterSelection = new TracksFilter.Selection(
                context.settingsService().getSettings().lastTracksFilterTag(),
                List.of());
        ContentPaneModeler modeler = new ContentPaneModeler();
        modeler.modelPane(this);

        this.tracksTable = new TracksTable(context, playlist, playerController, playlist::addTracks);
        JScrollPane tracksScrollPane = new JScrollPane(tracksTable);
        modeler.modelScrollPane(tracksScrollPane);
        tracksArea.add(tracksScrollPane, TRACKS_CARD);
        tracksArea.add(progressPanel, PROGRESS_CARD);
        modeler.modelTracksArea(tracksArea);
        context.indexService().addProgressListener(progress ->
                SwingUtilities.invokeLater(() -> showIndexingProgress(progress)));

        fileTree = new FileTree(context, this::showSelectedTreeFiles);
        TracksFilter tracksFilter = new TracksFilter(context, this::showFilteredFiles);
        JPanel fileBrowserPane = createFileBrowserPane(fileTree, tracksFilter, modeler);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1.0;
        c.insets = modeler.contentInsets();

        c.gridx = 0;
        c.weightx = 0.2;
        add(fileBrowserPane, c);
        c.gridx = 1;
        c.weightx = 0.6;
        add(tracksArea, c);
        c.gridx = 2;
        c.weightx = 0.2;
        add(createPlaylistBrowserPane(playlist, modeler), c);

    }

    Tag getSelectedFilterTag() {
        return filterSelection.tag();
    }

    List<TrackSort> getCurrentTracksSort() {
        return tracksTable.getCurrentSort();
    }

    String getCurrentTreePosition() {
        return fileTree.getCurrentPosition();
    }

    void goToPath(Path path) {
        List<IndexItem> files = fileTree.selectContainingDirectory(path);
        if (files.isEmpty()) {
            return;
        }

        treeSelectionActive = true;
        selectedTreeFiles = List.copyOf(files);
        loadTrackFiles(selectedTreeFiles, () -> tracksTable.selectPath(path));
    }

    private JPanel createFileBrowserPane(FileTree fileTree, TracksFilter tracksFilter, ContentPaneModeler modeler) {
        JPanel fileBrowserPane = new JPanel(new GridBagLayout());
        modeler.modelFileBrowserPane(fileBrowserPane);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.65;
        c.insets = modeler.fileTreeInsets();
        JScrollPane fileTreeScrollPane = new JScrollPane(fileTree);
        modeler.modelScrollPane(fileTreeScrollPane);
        fileBrowserPane.add(fileTreeScrollPane, c);

        c.gridy = 1;
        c.weighty = 0.35;
        c.insets = modeler.tracksFilterInsets();
        fileBrowserPane.add(tracksFilter, c);
        return fileBrowserPane;
    }

    private JPanel createPlaylistBrowserPane(PlaylistPane playlist, ContentPaneModeler modeler) {
        JPanel playlistBrowserPane = new JPanel(new GridBagLayout());
        modeler.modelPlaylistBrowserPane(playlistBrowserPane);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.68;
        c.insets = modeler.currentPlaylistInsets();
        playlistBrowserPane.add(playlist, c);

        c.gridy = 1;
        c.weighty = 0.32;
        c.insets = modeler.savedPlaylistsInsets();
        playlistBrowserPane.add(new SavedPlaylistsPane(context, playlist), c);
        return playlistBrowserPane;
    }

    private void showSelectedTreeFiles(List<IndexItem> files) {
        treeSelectionActive = true;
        selectedTreeFiles = files == null ? List.of() : List.copyOf(files);
        loadTrackFiles(resolveFilteredFiles());
    }

    private void showFilteredFiles(TracksFilter.Selection selection) {
        filterSelection = selection;
        loadTrackFiles(resolveFilteredFiles());
    }

    private List<IndexItem> resolveFilteredFiles() {
        if (filterSelection.values().isEmpty()) {
            return treeSelectionActive ? selectedTreeFiles : readLuceneFiles(filterSelection.tag(), List.of());
        }

        List<IndexItem> luceneFiles = readLuceneFiles(filterSelection.tag(), filterSelection.values());
        if (!treeSelectionActive) {
            return luceneFiles;
        }

        Set<Path> lucenePaths = luceneFiles.stream()
                .map(IndexItem::getPath)
                .collect(Collectors.toSet());
        return selectedTreeFiles.stream()
                .filter(file -> lucenePaths.contains(file.getPath()))
                .toList();
    }

    private List<IndexItem> readLuceneFiles(Tag tag, List<String> values) {
        try {
            return context.metadataIndexReaderService().readPaths(tag, values).stream()
                    .map(SoundFileIndexItem::new)
                    .map(IndexItem.class::cast)
                    .toList();
        } catch (IOException | SecurityException e) {
            ErrorReporter.log("Cannot read tracks filter paths.", e);
            JOptionPane.showMessageDialog(
                    this,
                    "Nie udalo sie wczytac plikow z indeksu metadanych.",
                    "Blad odczytu",
                    JOptionPane.ERROR_MESSAGE);
            return List.of();
        }
    }

    private void loadTrackFiles(List<IndexItem> files) {
        loadTrackFiles(files, null);
    }

    private void loadTrackFiles(List<IndexItem> files, Runnable loadedCallback) {
        if (files == null || files.isEmpty()) {
            tracksTable.showMappedFiles(files);
            if (loadedCallback != null) {
                loadedCallback.run();
            }
            showTracks();
            return;
        }

        showLoadingProgress(0, files.size());
        tracksTable.showMappedFiles(
                files,
                () -> {
                    showTracks();
                    if (loadedCallback != null) {
                        loadedCallback.run();
                    }
                },
                (processedFiles, totalFiles) ->
                        SwingUtilities.invokeLater(() -> showLoadingProgress(processedFiles, totalFiles)),
                () -> SwingUtilities.invokeLater(this::showTracks));
    }

    private void showLoadingProgress(int processedFiles, int totalFiles) {
        if (indexing) {
            return;
        }

        progressPanel.updateProgress("Wczytywanie...", processedFiles, totalFiles);
        tracksCardLayout.show(tracksArea, PROGRESS_CARD);
    }

    private void showTracks() {
        if (indexing) {
            return;
        }

        tracksCardLayout.show(tracksArea, TRACKS_CARD);
    }

    private void showIndexingProgress(IndexProgress progress) {
        indexing = progress.running();
        if (progress.running()) {
            progressPanel.updateProgress(progress.message(), progress.processedFiles(), progress.totalFiles());
            tracksCardLayout.show(tracksArea, PROGRESS_CARD);
            return;
        }

        showTracks();
    }

    private static class ProgressPanel extends JPanel {
        private final JLabel label = new JLabel("Wczytywanie...");
        private final JProgressBar progressBar = new JProgressBar();

        private ProgressPanel() {
            super(new GridBagLayout());
            ContentPaneModeler modeler = new ContentPaneModeler();
            modeler.modelProgressPanel(this);
            modeler.modelProgressLabel(label);
            modeler.modelProgressBar(progressBar);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.insets = modeler.progressLabelInsets();
            add(label, c);

            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            add(progressBar, c);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            UiTheme.paintRoundedBackground(this, graphics, getBackground());
            super.paintComponent(graphics);
        }

        private void updateProgress(String message, int processedFiles, int totalFiles) {
            label.setText(message);
            boolean determinate = totalFiles > 0;
            progressBar.setIndeterminate(!determinate);
            if (!determinate) {
                progressBar.setString("");
                return;
            }

            progressBar.setMinimum(0);
            progressBar.setMaximum(totalFiles);
            progressBar.setValue(processedFiles);
            progressBar.setString(processedFiles + " / " + totalFiles);
        }
    }
}
