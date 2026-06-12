package pl.khuzzuk;

import pl.khuzzuk.initialization.LoadingScreen;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.DocumentMapper;
import pl.khuzzuk.metadata.MetadataFieldKeyMapper;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.metadata.MetadataIndexWriterService;
import pl.khuzzuk.metadata.MetadataWriterService;
import pl.khuzzuk.metadata.MoodConverter;
import pl.khuzzuk.metadata.SoundFileMetadataMapper;
import pl.khuzzuk.playlist.SavedPlaylistMapper;
import pl.khuzzuk.playlist.SavedPlaylistService;
import pl.khuzzuk.player.FLACPlayer;
import pl.khuzzuk.player.MP3Player;
import pl.khuzzuk.player.OGGPlayer;
import pl.khuzzuk.player.SoundPlayer;
import pl.khuzzuk.player.SoundPlayerRouter;
import pl.khuzzuk.player.WAVPlayer;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.ui.MainWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MusicManager {
    private static final Path INDEX_PATH = Path.of("index.dat");
    private static final Path METADATA_INDEX_PATH = Path.of("metadata-index");
    private static final Path PLAYLISTS_PATH = Path.of("playlists");
    public static Context context;

    static void main() {
        SwingUtilities.invokeLater(MusicManager::showLoadingScreenAndInitialize);
    }

    private static void showLoadingScreenAndInitialize() {
        LoadingScreen loadingScreen = new LoadingScreen();
        loadingScreen.setVisible(true);

        new SwingWorker<Context, Void>() {
            @Override
            protected Context doInBackground() throws IOException {
                return initComponents();
            }

            @Override
            protected void done() {
                try {
                    context = get();
                    loadingScreen.setVisible(false);
                    loadingScreen.dispose();
                    showMainWindow();
                } catch (Exception e) {
                    loadingScreen.setVisible(false);
                    JOptionPane.showMessageDialog(
                            loadingScreen,
                            "Nie udalo sie zainicjalizowac aplikacji.\n" + userMessage(e),
                            "Blad inicjalizacji",
                            JOptionPane.ERROR_MESSAGE);
                    loadingScreen.dispose();
                    System.exit(-1);
                }
            }
        }.execute();
    }

    private static Context initComponents() throws IOException {
        SettingsService settingsService = new SettingsService(new SettingsToPropertiesMapper());
        MoodConverter moodConverter = new MoodConverter();
        SoundFileMetadataMapper soundFileMetadataMapper = new SoundFileMetadataMapper(moodConverter);
        MetadataReaderService metadataReaderService = new MetadataReaderService(soundFileMetadataMapper);
        MetadataFieldKeyMapper metadataFieldKeyMapper = new MetadataFieldKeyMapper();
        MetadataWriterService metadataWriterService = new MetadataWriterService(metadataFieldKeyMapper);
        DocumentMapper documentMapper = new DocumentMapper();
        MetadataIndexReaderService metadataIndexReaderService =
                new MetadataIndexReaderService(METADATA_INDEX_PATH, documentMapper);
        MetadataIndexWriterService metadataIndexWriterService =
                new MetadataIndexWriterService(METADATA_INDEX_PATH, documentMapper);
        createIndexFileIfMissing();
        IndexService indexService = new IndexService(
                INDEX_PATH,
                metadataReaderService,
                metadataIndexReaderService,
                metadataIndexWriterService);
        IndexReaderService indexReaderService = new IndexReaderService(INDEX_PATH);
        indexReaderService.read();
        SavedPlaylistService savedPlaylistService =
                new SavedPlaylistService(PLAYLISTS_PATH, new SavedPlaylistMapper());
        SoundPlayer soundPlayer = new SoundPlayerRouter(
                new MP3Player(),
                new FLACPlayer(),
                new WAVPlayer(),
                new OGGPlayer());
        return new Context(
                settingsService,
                metadataReaderService,
                metadataWriterService,
                metadataIndexReaderService,
                metadataIndexWriterService,
                indexService,
                indexReaderService,
                savedPlaylistService,
                soundPlayer);
    }

    private static String userMessage(Exception exception) {
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        if (exception.getMessage() != null) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }

    private static void showMainWindow() {
        MainWindow mainWindow = new MainWindow(context);
        mainWindow.setVisible(true);
    }

    private static void createIndexFileIfMissing() throws IOException {
        if (Files.notExists(INDEX_PATH)) {
            Files.createFile(INDEX_PATH);
        }
    }
}
