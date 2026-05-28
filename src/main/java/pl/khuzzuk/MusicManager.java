package pl.khuzzuk;

import pl.khuzzuk.initialization.LoadingScreen;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.ui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MusicManager {
    private static final Path INDEX_PATH = Path.of("index.dat");
    public static SettingsService settingsService;
    public static MetadataReaderService metadataReaderService;
    public static IndexService indexService;
    public static IndexReaderService indexReaderService;

    static void main() {
        SwingUtilities.invokeLater(MusicManager::initComponents);
        SwingUtilities.invokeLater(MusicManager::showMainWindow);
    }

    private static void initComponents() {
        LoadingScreen loadingScreen = new LoadingScreen();
        loadingScreen.setVisible(true);

        try {
            settingsService = new SettingsService(new SettingsToPropertiesMapper());
            metadataReaderService = new MetadataReaderService();
            createIndexFileIfMissing();
            indexService = new IndexService(INDEX_PATH, metadataReaderService);
            indexReaderService = new IndexReaderService(INDEX_PATH);
            indexReaderService.read();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    loadingScreen,
                    "Nie udalo sie zainicjalizowac aplikacji.\n" + e.getMessage(),
                    "Blad inicjalizacji",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }

        loadingScreen.setVisible(false);
    }

    private static void showMainWindow() {
        MainWindow mainWindow = new MainWindow(settingsService, indexService, indexReaderService);
        mainWindow.setVisible(true);
    }

    private static void createIndexFileIfMissing() throws IOException {
        if (Files.notExists(INDEX_PATH)) {
            Files.createFile(INDEX_PATH);
        }
    }
}
