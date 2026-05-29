package pl.khuzzuk.player;

public class SoundFile {
    private final String path;
    private final String title;

    public SoundFile(String path, String title) {
        this.path = path;
        this.title = title;
    }

    public String path() {
        return path;
    }

    public String title() {
        return title;
    }
}
