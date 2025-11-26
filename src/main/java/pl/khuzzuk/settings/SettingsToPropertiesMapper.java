package pl.khuzzuk.settings;

import java.util.Properties;

public class SettingsToPropertiesMapper {
    private static final String WINDOW_X_PROPERTY = "window.x";
    private static final String WINDOW_Y_PROPERTY = "window.y";
    private static final String WINDOW_WIDTH_PROPERTY = "window.width";
    private static final String WINDOW_HEIGHT_PROPERTY = "window.height";
    private static final String WINDOW_MAXIMIZE_PROPERTY = "window.maximize";
    private static final String LAST_TREE_POSITION_PROPERTY = "last.tree.position";
    private static final String LAST_PLAYLIST_PROPERTY = "last.playlist";

    public Settings toSettings(Properties prop) {
        return new Settings(
                getInt(prop.getProperty(WINDOW_X_PROPERTY), 100),
                getInt(prop.getProperty(WINDOW_Y_PROPERTY), 100),
                getInt(prop.getProperty(WINDOW_WIDTH_PROPERTY), 600),
                getInt(prop.getProperty(WINDOW_HEIGHT_PROPERTY), 400),
                getBoolean(prop.getProperty(WINDOW_MAXIMIZE_PROPERTY), false),
                prop.getProperty(LAST_TREE_POSITION_PROPERTY, ""),
                prop.getProperty(LAST_PLAYLIST_PROPERTY, "")
        );
    }

    public Properties toProperties(Settings settings) {
        Properties prop = new Properties();
        prop.setProperty(WINDOW_X_PROPERTY, Integer.toString(settings.windowX()));
        prop.setProperty(WINDOW_Y_PROPERTY, Integer.toString(settings.windowY()));
        prop.setProperty(WINDOW_WIDTH_PROPERTY, Integer.toString(settings.windowWidth()));
        prop.setProperty(WINDOW_HEIGHT_PROPERTY, Integer.toString(settings.windowHeight()));
        prop.setProperty(WINDOW_MAXIMIZE_PROPERTY, Boolean.toString(settings.maximizedWindow()));
        prop.setProperty(LAST_TREE_POSITION_PROPERTY, settings.lastTreePosition());
        prop.setProperty(LAST_PLAYLIST_PROPERTY, settings.lastPlaylist());
        return prop;
    }

    private static int getInt(String prop, int defaultValue) {
        try {
            return Integer.parseInt(prop);
        }  catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(String prop, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(prop);
        }   catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
