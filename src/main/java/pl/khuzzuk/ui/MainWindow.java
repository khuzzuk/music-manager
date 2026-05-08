package pl.khuzzuk.ui;

import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.JFrame;
import java.awt.BorderLayout;

public class MainWindow extends JFrame {
    SettingsService settingsService;

    public MainWindow(SettingsService settingsService) {
        super("Music Manager");
        this.settingsService = settingsService;

        Settings settings = settingsService.getSettings();
        setBounds(settings.windowX(),  settings.windowY(), settings.windowWidth(), settings.windowHeight());

        setLayout(new BorderLayout(5, 5));
        ContentPane contentPane = new ContentPane();
        add(contentPane, BorderLayout.CENTER);
        PlayerPane playerPane = new PlayerPane();
        add(playerPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new CloseAppListener(this, settingsService));
    }
}
