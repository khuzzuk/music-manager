package pl.khuzzuk.settings;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SettingsToPropertiesMapper {
    private static final String WINDOW_X_PROPERTY = "window.x";
    private static final String WINDOW_Y_PROPERTY = "window.y";
    private static final String WINDOW_WIDTH_PROPERTY = "window.width";
    private static final String WINDOW_HEIGHT_PROPERTY = "window.height";
    private static final String WINDOW_MAXIMIZE_PROPERTY = "window.maximize";
    private static final String LAST_TREE_POSITION_PROPERTY = "last.tree.position";
    private static final String LAST_PLAYLIST_PROPERTY = "last.playlist";
    private static final String INDEXED_PATHS_PROPERTY = "indexed.paths";
    private static final String LAST_CHOOSEN_PATH_PROPERTY = "last.choosen.path";

    public Settings toSettings(Properties prop) {
        return new Settings(
                getInt(prop.getProperty(WINDOW_X_PROPERTY), 100),
                getInt(prop.getProperty(WINDOW_Y_PROPERTY), 100),
                getInt(prop.getProperty(WINDOW_WIDTH_PROPERTY), 600),
                getInt(prop.getProperty(WINDOW_HEIGHT_PROPERTY), 400),
                getBoolean(prop.getProperty(WINDOW_MAXIMIZE_PROPERTY)),
                prop.getProperty(LAST_TREE_POSITION_PROPERTY, ""),
                prop.getProperty(LAST_PLAYLIST_PROPERTY, ""),
                getPaths(prop),
                Path.of(prop.getProperty(LAST_CHOOSEN_PATH_PROPERTY, ""))
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
        prop.setProperty(INDEXED_PATHS_PROPERTY, settings.indexedPaths().stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator)));
        prop.setProperty(LAST_CHOOSEN_PATH_PROPERTY, settings.lastChoosenPath().toString());
        return prop;
    }

    private static List<Path> getPaths(Properties prop) {
        String paths = prop.getProperty(INDEXED_PATHS_PROPERTY, "");
        if (paths.isBlank()) {
            return List.of();
        }
        return Arrays.stream(paths.split(Pattern.quote(File.pathSeparator)))
                .filter(path -> !path.isBlank())
                .map(Path::of)
                .toList();
    }

    private static int getInt(String prop, int defaultValue) {
        try {
            return Integer.parseInt(prop);
        }  catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getBoolean(String prop) {
        try {
            return Boolean.parseBoolean(prop);
        }   catch (NumberFormatException e) {
            return false;
        }
    }
}
