package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.Tag;

import java.util.List;

final class MetadataSuggestionTags {
    private static final List<Tag> TAGS = List.of(
            Tag.ARTIST,
            Tag.COMPOSER,
            Tag.CONDUCTOR,
            Tag.GENRE,
            Tag.MOOD,
            Tag.TEMPO,
            Tag.OCCASION);

    private MetadataSuggestionTags() {
    }

    static boolean supports(Tag tag) {
        return TAGS.contains(tag);
    }
}
