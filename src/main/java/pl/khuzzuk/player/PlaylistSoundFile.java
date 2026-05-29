package pl.khuzzuk.player;

public class PlaylistSoundFile {
    private final SoundFile soundFile;
    private PlaylistSoundFile previous;
    private PlaylistSoundFile next;

    public PlaylistSoundFile(SoundFile soundFile) {
        this.soundFile = soundFile;
    }

    public SoundFile soundFile() {
        return soundFile;
    }

    public PlaylistSoundFile previous() {
        return previous;
    }

    public void setPrevious(PlaylistSoundFile previous) {
        this.previous = previous;
    }

    public PlaylistSoundFile next() {
        return next;
    }

    public void setNext(PlaylistSoundFile next) {
        this.next = next;
    }
}
