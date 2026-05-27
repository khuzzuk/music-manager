package pl.khuzzuk.ui;

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
import java.awt.BorderLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IndexDirectoriesDialog extends JDialog {
    private final SettingsService settingsService;

    public IndexDirectoriesDialog(Window owner, SettingsService settingsService) {
        super(owner, "Indeksowane katalogi");
        this.settingsService = settingsService;
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

        JButton addButton = new JButton("Dodaj");
        addButton.addActionListener(event -> addIndexDirectory());
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
                    selectedPath);

            try {
                settingsService.saveSettings(newSettings);
                refresh();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "Nie udalo sie zapisac ustawien indeksowania.",
                        "Blad zapisu",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
