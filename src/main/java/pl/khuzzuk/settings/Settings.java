package pl.khuzzuk.settings;

import java.io.IOException;

public record Settings(
        int windowX,
        int windowY,
        int windowWidth,
        int windowHeight,
        boolean maximizedWindow,
        String lastTreePosition,
        String lastPlaylist) {
}
