package pl.khuzzuk.metadata;

import pl.khuzzuk.player.SoundFileType;

import java.util.Locale;

public class SoundFileMetadataUpdateMapper {
    public void setValue(SoundFileMetadata metadata, Tag tag, String value) {
        setValue(metadata, tag, (Object) value);
    }

    public void setValue(SoundFileMetadata metadata, Tag tag, Object value) {
        switch (tag) {
            case FORMAT -> metadata.setFormat(toFormat(value));
            case RATING -> metadata.setRating(toRating(value));
            case DURATION -> metadata.setDurationSeconds(toDurationSeconds(value));
            case TITLE -> metadata.setTitle(toText(value));
            case DATE -> metadata.setDate(toText(value));
            case ARTIST -> metadata.setArtist(toText(value));
            case ARTISTS -> metadata.setArtists(toText(value));
            case ALBUM -> metadata.setAlbum(toText(value));
            case ALBUM_ARTIST -> metadata.setAlbumArtist(toText(value));
            case ALBUM_ARTISTS -> metadata.setAlbumArtists(toText(value));
            case COMPOSER -> metadata.setComposer(toText(value));
            case CONDUCTOR -> metadata.setConductor(toText(value));
            case COUNTRY -> metadata.setCountry(toText(value));
            case CUSTOM1 -> metadata.setCustom1(toText(value));
            case CUSTOM2 -> metadata.setCustom2(toText(value));
            case CUSTOM3 -> metadata.setCustom3(toText(value));
            case CUSTOM4 -> metadata.setCustom4(toText(value));
            case CUSTOM5 -> metadata.setCustom5(toText(value));
            case DISC_NO -> metadata.setDiscNo(toText(value));
            case GENRE -> metadata.setGenre(toText(value));
            case GROUP -> metadata.setGroup(toText(value));
            case INSTRUMENT -> metadata.setInstrument(toText(value));
            case MOOD -> metadata.setMood(toText(value));
            case MOVEMENT -> metadata.setMovement(toText(value));
            case OCCASION -> metadata.setOccasion(toText(value));
            case OPUS -> metadata.setOpus(toText(value));
            case ORCHESTRA -> metadata.setOrchestra(toText(value));
            case QUALITY -> metadata.setQuality(toText(value));
            case RANKING -> metadata.setRanking(toText(value));
            case TEMPO -> metadata.setTempo(toText(value));
            case TONALITY -> metadata.setTonality(toText(value));
            case TRACK -> metadata.setTrack(toText(value));
            case WORK -> metadata.setWork(toText(value));
            case WORK_TYPE -> metadata.setWorkType(toText(value));
        }
    }

    private SoundFileType toFormat(Object value) {
        if (value instanceof SoundFileType format) {
            return format;
        }
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Format cannot be blank");
        }

        return SoundFileType.valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
    }

    private int toRating(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return 0;
        }

        return Integer.parseInt(value.toString().trim());
    }

    private int toDurationSeconds(Object value) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value == null || value.toString().isBlank()) {
            return 0;
        }

        return Math.max(0, Integer.parseInt(value.toString().trim()));
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }
}
