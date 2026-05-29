package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.JFrame;
import java.awt.BorderLayout;

public class MainWindow extends JFrame {
    SettingsService settingsService;

    public MainWindow(Context context) {
        super("Music Manager");
        this.settingsService = context.settingsService();

        Settings settings = context.settingsService().getSettings();
        setBounds(settings.windowX(),  settings.windowY(), settings.windowWidth(), settings.windowHeight());

        setLayout(new BorderLayout(5, 5));
        PlaylistPane playlistPane = new PlaylistPane();
        PlayerController playerController = new PlayerController(context.soundPlayer(), playlistPane);
        ContentPane contentPane = new ContentPane(context, playlistPane);
        add(contentPane, BorderLayout.CENTER);
        PlayerPane playerPane = new PlayerPane(playerController);
        add(playerPane, BorderLayout.SOUTH);

        MainMenuBar mainMenuBar = new MainMenuBar(context);
        setJMenuBar(mainMenuBar);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new CloseAppListener(this, context));
    }
}
