package pl.khuzzuk.settings;

import java.nio.file.Path;
import java.util.List;

public record Settings(
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight,
        boolean maximizedWindow,
        String lastTreePosition,
        String lastPlaylist,
        List<Path> indexedPaths,
        Path lastChoosenPath,
        List<TrackColumn> trackColumns) {
    public Settings {
        indexedPaths = indexedPaths == null ? List.of() : List.copyOf(indexedPaths);
        lastChoosenPath = lastChoosenPath == null ? Path.of("") : lastChoosenPath;
        trackColumns = trackColumns == null ? List.of() : List.copyOf(trackColumns);
    }
}
