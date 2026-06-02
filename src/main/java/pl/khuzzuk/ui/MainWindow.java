package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.settings.Settings;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class MainWindow extends JFrame {
    private static final String PLAY_PAUSE_ACTION = "playPause";
    SettingsService settingsService;

    public MainWindow(Context context) {
        super("Music Manager");
        this.settingsService = context.settingsService();
        MainWindowModeler modeler = new MainWindowModeler();
        modeler.modelWindow(this);

        Settings settings = context.settingsService().getSettings();
        setBounds(settings.windowX(),  settings.windowY(), settings.windowWidth(), settings.windowHeight());

        setLayout(new BorderLayout(5, 5));
        PlaylistPane playlistPane = new PlaylistPane();
        PlayerController playerController = new PlayerController(context.soundPlayer(), playlistPane);
        ContentPane contentPane = new ContentPane(context, playlistPane);
        add(contentPane, BorderLayout.CENTER);
        PlayerPane playerPane = new PlayerPane(playerController);
        JPanel playerPaneContainer = new JPanel(new BorderLayout());
        modeler.modelPlayerPaneContainer(playerPaneContainer);
        playerPaneContainer.add(playerPane, BorderLayout.CENTER);
        add(playerPaneContainer, BorderLayout.SOUTH);
        registerPlayPauseAction(playerPane);

        MainMenuBar mainMenuBar = new MainMenuBar(context);
        setJMenuBar(mainMenuBar);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new CloseAppListener(this, context));
    }

    private void registerPlayPauseAction(PlayerPane playerPane) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (!isActive()
                    || event.getID() != KeyEvent.KEY_PRESSED
                    || event.getKeyCode() != KeyEvent.VK_SPACE
                    || !event.isControlDown()) {
                return false;
            }

            playerPane.playPause();
            return true;
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK),
                PLAY_PAUSE_ACTION);
        getRootPane().getActionMap().put(PLAY_PAUSE_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                playerPane.playPause();
            }
        });
    }
}
