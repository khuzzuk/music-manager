package pl.khuzzuk.settings;

import java.nio.file.Path;
import java.util.List;
import pl.khuzzuk.metadata.Tag;

public record Settings(
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight,
        boolean maximizedWindow,
        String lastTreePosition,
        String lastPlaylist,
        int lastPlaylistPosition,
        List<Path> indexedPaths,
        Path lastChoosenPath,
        List<TrackColumn> trackColumns,
        List<TrackSort> trackSort,
        Tag lastTracksFilterTag) {
    public Settings {
        indexedPaths = indexedPaths == null ? List.of() : List.copyOf(indexedPaths);
        lastChoosenPath = lastChoosenPath == null ? Path.of("") : lastChoosenPath;
        trackColumns = trackColumns == null ? List.of() : List.copyOf(trackColumns);
        trackSort = trackSort == null ? List.of() : List.copyOf(trackSort);
        lastTracksFilterTag = lastTracksFilterTag == null ? Tag.MOOD : lastTracksFilterTag;
    }
}
