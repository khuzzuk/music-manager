package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.JFrame;
import java.awt.BorderLayout;

public class MainWindow extends JFrame {
    SettingsService settingsService;

    public MainWindow(
            SettingsService settingsService,
            IndexService indexService,
            IndexReaderService indexReaderService,
            MetadataReaderService metadataReaderService) {
        super("Music Manager");
        this.settingsService = settingsService;

        Settings settings = settingsService.getSettings();
        setBounds(settings.windowX(),  settings.windowY(), settings.windowWidth(), settings.windowHeight());

        setLayout(new BorderLayout(5, 5));
        ContentPane contentPane = new ContentPane(settingsService, indexReaderService, indexService, metadataReaderService);
        add(contentPane, BorderLayout.CENTER);
        PlayerPane playerPane = new PlayerPane();
        add(playerPane, BorderLayout.SOUTH);

        MainMenuBar mainMenuBar = new MainMenuBar(settingsService, indexService, indexReaderService);
        setJMenuBar(mainMenuBar);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new CloseAppListener(this, settingsService));
    }
}
