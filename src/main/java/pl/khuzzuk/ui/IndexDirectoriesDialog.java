package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.index.RootIndexItem;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IndexDirectoriesDialog extends JDialog {
    private final SettingsService settingsService;
    private final IndexService indexService;
    private final IndexReaderService indexReaderService;
    private JButton addButton;

    public IndexDirectoriesDialog(Window owner, Context context) {
        super(owner, "Indeksowane katalogi");
        this.settingsService = context.settingsService();
        this.indexService = context.indexService();
        this.indexReaderService = context.indexReaderService();
        setSize(500, 300);
    }

    public void showDialog() {
        refresh();
        setVisible(true);
        toFront();
    }

    private void refresh() {
        getContentPane().removeAll();
        setLayout(new BorderLayout(5, 5));

        DefaultListModel<Path> indexedPathsModel = new DefaultListModel<>();
        settingsService.getSettings().indexedPaths().forEach(indexedPathsModel::addElement);
        JList<Path> indexedPathsList = new JList<>(indexedPathsModel);
        add(new JScrollPane(indexedPathsList), BorderLayout.CENTER);

        addButton = new JButton("Dodaj");
        addButton.addActionListener(ignored -> addIndexDirectory());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private void addIndexDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Wybierz katalog do indeksowania");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        Path lastChoosenPath = settingsService.getSettings().lastChoosenPath();
        if (lastChoosenPath != null && !lastChoosenPath.toString().isBlank()) {
            fileChooser.setCurrentDirectory(lastChoosenPath.toFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            Path selectedPath = selectedDirectory.toPath();
            Settings settings = settingsService.getSettings();
            List<Path> indexedPaths = new ArrayList<>(settings.indexedPaths());
            if (!indexedPaths.contains(selectedPath)) {
                indexedPaths.add(selectedPath);
            }

            Settings newSettings = new Settings(
                    settings.windowX(),
                    settings.windowY(),
                    settings.windowWidth(),
                    settings.windowHeight(),
                    settings.maximizedWindow(),
                    settings.lastTreePosition(),
                    settings.lastPlaylist(),
                    indexedPaths,
                    selectedPath,
                    settings.trackColumns());

            try {
                settingsService.saveSettings(newSettings);
            } catch (IOException e) {
                ErrorReporter.log("Cannot save settings.", e);
                JOptionPane.showMessageDialog(
                        this,
                        "Nie udalo sie zapisac ustawien.\n"
                                + ErrorReporter.userMessage(e)
                                + "\n\nStack trace zapisano w " + ErrorReporter.logFile(),
                        "Blad zapisu",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            startIndexing(indexedPaths, addButton);
        }
    }

    private void startIndexing(List<Path> indexedPaths, JButton addButton) {
        addButton.setEnabled(false);
        RootIndexItem root = indexReaderService.getCurrentRootIndexItem();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                indexService.index(root, indexedPaths);
                return null;
            }

            @Override
            protected void done() {
                addButton.setEnabled(true);
                try {
                    get();
                    refresh();
                } catch (Exception e) {
                    ErrorReporter.log("Cannot write index.", e);
                    JOptionPane.showMessageDialog(
                            IndexDirectoriesDialog.this,
                            "Nie udalo sie zapisac indeksu.\n"
                                    + ErrorReporter.userMessage(e)
                                    + "\n\nStack trace zapisano w " + ErrorReporter.logFile(),
                            "Blad indeksowania",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
