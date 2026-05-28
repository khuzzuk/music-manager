package pl.khuzzuk.player;

import java.util.Set;

public enum SoundFileType {
    MP3, FLAC;

    public static final Set<String> EXTENSIONS = Set.of("mp3", "flac");
}
