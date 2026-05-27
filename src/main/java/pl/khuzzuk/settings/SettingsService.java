package pl.khuzzuk.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsService {
    private Settings settings;
    private final SettingsToPropertiesMapper settingsToPropertiesMapper;

    public SettingsService(SettingsToPropertiesMapper settingsToPropertiesMapper) throws IOException {
        this.settingsToPropertiesMapper = settingsToPropertiesMapper;
        loadSettings();
    }

    private void loadSettings() throws IOException {
        Properties props = new Properties();
        Path path = getSettingsPath();
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            props.load(inputStream);
        }
        settings = settingsToPropertiesMapper.toSettings(props);
    }

    public void saveSettings(Settings settings) throws IOException {
        Properties props = settingsToPropertiesMapper.toProperties(settings);
        Path path = getSettingsPath();
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "Settings");
        }
        this.settings = settings;
    }

    private Path getSettingsPath() {
        return Paths.get("settings.properties");
    }

    public  Settings getSettings() {
        return settings;
    }
}
