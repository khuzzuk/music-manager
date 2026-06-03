package pl.khuzzuk.playlist;

import java.nio.file.Path;

public record SavedPlaylist(String name, Path path) {
}
