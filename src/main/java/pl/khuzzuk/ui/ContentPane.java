package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexProgress;
import pl.khuzzuk.index.IndexItem;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class ContentPane extends JPanel {
    private static final String TRACKS_CARD = "tracks";
    private static final String PROGRESS_CARD = "progress";

    private final CardLayout tracksCardLayout = new CardLayout();
    private final JPanel tracksArea = new JPanel(tracksCardLayout);
    private final TracksTable tracksTable;
    private final ProgressPanel progressPanel = new ProgressPanel();
    private boolean indexing;

    public ContentPane(Context context, PlaylistPane playlist) {
        super(new GridBagLayout());

        this.tracksTable = new TracksTable(context, playlist::addTracks);
        tracksArea.add(new JScrollPane(tracksTable), TRACKS_CARD);
        tracksArea.add(progressPanel, PROGRESS_CARD);
        context.indexService().addProgressListener(progress ->
                SwingUtilities.invokeLater(() -> showIndexingProgress(progress)));

        FileTree fileTree = new FileTree(context, this::showMappedFiles);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1.0;
        c.insets = new Insets(0, 5, 0, 5);

        c.gridx = 0;
        c.weightx = 0.2;
        add(fileTree, c);
        c.gridx = 1;
        c.weightx = 0.6;
        add(tracksArea, c);
        c.gridx = 2;
        c.weightx = 0.2;
        add(playlist, c);
    }

    private void showMappedFiles(List<IndexItem> files) {
        if (files == null || files.isEmpty()) {
            tracksTable.showMappedFiles(files);
            showTracks();
            return;
        }

        showLoadingProgress(0, files.size());
        tracksTable.showMappedFiles(
                files,
                this::showTracks,
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
            progressBar.setStringPainted(true);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.insets = new Insets(0, 20, 8, 20);
            add(label, c);

            c.gridy = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            add(progressBar, c);
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
