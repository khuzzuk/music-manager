package pl.khuzzuk.metadata;

import java.util.Locale;
import java.util.function.Function;

public enum Tag {
    FORMAT("format", "Format", SoundFileMetadata::format),
    TITLE("title", "Tytul", SoundFileMetadata::title),
    DURATION("duration", "Czas", SoundFileMetadata::duration),
    DATE("date", "Data", SoundFileMetadata::date),
    ARTIST("artist", "Artysta", SoundFileMetadata::artist),
    ARTISTS("artists", "Artysci", SoundFileMetadata::artists),
    ALBUM("album", "Album", SoundFileMetadata::album),
    ALBUM_ARTIST("albumArtist", "Artysta albumu", SoundFileMetadata::albumArtist),
    ALBUM_ARTISTS("albumArtists", "Artysci albumu", SoundFileMetadata::albumArtists),
    COMPOSER("composer", "Kompozytor", SoundFileMetadata::composer),
    CONDUCTOR("conductor", "Dyrygent", SoundFileMetadata::conductor),
    COUNTRY("country", "Kraj", SoundFileMetadata::country),
    CUSTOM1("custom1", "Custom 1", SoundFileMetadata::custom1),
    CUSTOM2("custom2", "Custom 2", SoundFileMetadata::custom2),
    CUSTOM3("custom3", "Custom 3", SoundFileMetadata::custom3),
    CUSTOM4("custom4", "Custom 4", SoundFileMetadata::custom4),
    CUSTOM5("custom5", "Custom 5", SoundFileMetadata::custom5),
    DISC_NO("discNo", "Nr plyty", SoundFileMetadata::discNo),
    GENRE("genre", "Gatunek", SoundFileMetadata::genre),
    GROUP("group", "Grupa", SoundFileMetadata::group),
    INSTRUMENT("instrument", "Instrument", SoundFileMetadata::instrument),
    RATING("rating", "Rating", SoundFileMetadata::rating),
    MOOD("mood", "Mood", SoundFileMetadata::mood),
    MOVEMENT("movement", "Movement", SoundFileMetadata::movement),
    OCCASION("occasion", "Occasion", SoundFileMetadata::occasion),
    OPUS("opus", "Opus", SoundFileMetadata::opus),
    ORCHESTRA("orchestra", "Orkiestra", SoundFileMetadata::orchestra),
    QUALITY("quality", "Jakosc", SoundFileMetadata::quality),
    RANKING("ranking", "Ranking", SoundFileMetadata::ranking),
    TEMPO("tempo", "Tempo", SoundFileMetadata::tempo),
    TONALITY("tonality", "Tonacja", SoundFileMetadata::tonality),
    TRACK("track", "Sciezka", SoundFileMetadata::track),
    WORK("work", "Utwor", SoundFileMetadata::work),
    WORK_TYPE("workType", "Typ utworu", SoundFileMetadata::workType);

    private final String settingsName;
    private final String label;
    private final Function<SoundFileMetadata, Object> valueProvider;

    Tag(String settingsName, String label, Function<SoundFileMetadata, Object> valueProvider) {
        this.settingsName = settingsName;
        this.label = label;
        this.valueProvider = valueProvider;
    }

    public static Tag fromSettingsName(String settingsName) {
        if (settingsName == null) {
            return null;
        }

        String normalizedName = settingsName.trim().toLowerCase(Locale.ROOT);
        for (Tag tag : values()) {
            if (tag.settingsName.toLowerCase(Locale.ROOT).equals(normalizedName)) {
                return tag;
            }
        }

        return null;
    }

    public String settingsName() {
        return settingsName;
    }

    public String label() {
        return label;
    }

    public Object getValue(SoundFileMetadata metadata) {
        return valueProvider.apply(metadata);
    }

    public boolean isNumeric() {
        return this == RATING;
    }
}
