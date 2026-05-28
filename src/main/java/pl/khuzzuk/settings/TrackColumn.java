package pl.khuzzuk.settings;

public record TrackColumn(String name, int width) {
    private static final int DEFAULT_WIDTH = 120;

    public TrackColumn {
        name = name == null ? "" : name.trim();
        width = width > 0 ? width : DEFAULT_WIDTH;
    }
}
