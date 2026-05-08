package pl.khuzzuk.player;

public record PlaylistSoundFile (SoundFile soundFile, PlaylistSoundFile previous,  PlaylistSoundFile next) {
}
