package pl.khuzzuk;

import pl.khuzzuk.initialization.LoadingScreen;
import pl.khuzzuk.settings.SettingsService;
import pl.khuzzuk.settings.SettingsToPropertiesMapper;
import pl.khuzzuk.ui.MainWindow;

import javax.swing.SwingUtilities;
import java.io.IOException;

public class Main {
    public static SettingsService settingsService;

    static void main() {
        SwingUtilities.invokeLater(Main::initComponents);
        SwingUtilities.invokeLater(Main::showMainWindow);
    }

    private static void initComponents() {
        LoadingScreen loadingScreen = new LoadingScreen();
        loadingScreen.setVisible(true);

        try {
            settingsService = new SettingsService(new SettingsToPropertiesMapper());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        loadingScreen.setVisible(false);
    }

    private static void showMainWindow() {
        MainWindow mainWindow = new MainWindow(settingsService);
        mainWindow.setVisible(true);
    }
}
