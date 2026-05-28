package pl.khuzzuk.settings;

import pl.khuzzuk.metadata.Tag;

public record TrackColumn(Tag tag, int width) {
    private static final int DEFAULT_WIDTH = 120;

    public TrackColumn {
        width = width > 0 ? width : DEFAULT_WIDTH;
    }
}
