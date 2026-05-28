package pl.khuzzuk;

import pl.khuzzuk.initialization.LoadingScreen;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.DocumentMapper;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.MoodConverter;
import pl.khuzzuk.metadata.SoundFileMetadataMapper;
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
    private static final Path METADATA_INDEX_PATH = Path.of("metadata-index");
    public static SettingsService settingsService;
    public static MoodConverter moodConverter;
    public static SoundFileMetadataMapper soundFileMetadataMapper;
    public static MetadataReaderService metadataReaderService;
    public static MetadataWriterService metadataWriterService;
    public static DocumentMapper documentMapper;
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
            moodConverter = new MoodConverter();
            soundFileMetadataMapper = new SoundFileMetadataMapper(moodConverter);
            metadataReaderService = new MetadataReaderService(soundFileMetadataMapper);
            documentMapper = new DocumentMapper();
            metadataWriterService = new MetadataWriterService(METADATA_INDEX_PATH, documentMapper);
            createIndexFileIfMissing();
            indexService = new IndexService(INDEX_PATH, metadataReaderService, metadataWriterService);
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
        MainWindow mainWindow = new MainWindow(settingsService, indexService, indexReaderService, metadataReaderService);
        mainWindow.setVisible(true);
    }

    private static void createIndexFileIfMissing() throws IOException {
        if (Files.notExists(INDEX_PATH)) {
            Files.createFile(INDEX_PATH);
        }
    }
}
