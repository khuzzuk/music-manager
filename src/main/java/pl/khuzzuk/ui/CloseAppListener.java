package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.JOptionPane;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class CloseAppListener extends WindowAdapter {
    private final MainWindow mainWindow;
    private final SettingsService settingsService;

    public CloseAppListener(MainWindow mainWindow, Context context) {
        this.mainWindow = mainWindow;
        this.settingsService = context.settingsService();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        Rectangle bounds = mainWindow.getBounds();
        Settings oldSettings = settingsService.getSettings();
        Settings newSettings = new Settings(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                oldSettings.maximizedWindow(),
                oldSettings.lastTreePosition(),
                oldSettings.lastPlaylist(),
                oldSettings.indexedPaths(),
                oldSettings.lastChoosenPath(),
                oldSettings.trackColumns());
        try {
            settingsService.saveSettings(newSettings);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    mainWindow,
                    "Nie udalo sie zapisac ustawien aplikacji.",
                    "Blad zapisu",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
