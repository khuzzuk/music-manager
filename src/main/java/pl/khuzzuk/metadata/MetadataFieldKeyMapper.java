package pl.khuzzuk.metadata;

import org.jaudiotagger.tag.FieldKey;

public class MetadataFieldKeyMapper {
    public FieldKey toFieldKey(Tag tag) {
        return switch (tag) {
            case FORMAT -> null;
            case FILE_NAME -> null;
            case DURATION -> null;
            case TITLE -> FieldKey.TITLE;
            case DATE -> FieldKey.RECORDINGDATE;
            case ARTIST -> FieldKey.ARTIST;
            case ARTISTS -> FieldKey.ARTISTS;
            case ALBUM -> FieldKey.ALBUM;
            case ALBUM_ARTIST -> FieldKey.ALBUM_ARTIST;
            case ALBUM_ARTISTS -> FieldKey.ALBUM_ARTISTS;
            case COMPOSER -> FieldKey.COMPOSER;
            case CONDUCTOR -> FieldKey.CONDUCTOR;
            case COUNTRY -> FieldKey.COUNTRY;
            case CUSTOM1 -> FieldKey.CUSTOM1;
            case CUSTOM2 -> FieldKey.CUSTOM2;
            case CUSTOM3 -> FieldKey.CUSTOM3;
            case CUSTOM4 -> FieldKey.CUSTOM4;
            case CUSTOM5 -> FieldKey.CUSTOM5;
            case DISC_NO -> FieldKey.DISC_NO;
            case GENRE -> FieldKey.GENRE;
            case GROUP -> FieldKey.GROUP;
            case INSTRUMENT -> FieldKey.INSTRUMENT;
            case RATING -> FieldKey.RATING;
            case MOOD -> FieldKey.MOOD;
            case MOVEMENT -> FieldKey.MOVEMENT;
            case OCCASION -> FieldKey.OCCASION;
            case OPUS -> FieldKey.OPUS;
            case ORCHESTRA -> FieldKey.ORCHESTRA;
            case QUALITY -> FieldKey.QUALITY;
            case RANKING -> FieldKey.RANKING;
            case TEMPO -> FieldKey.TEMPO;
            case TONALITY -> FieldKey.TONALITY;
            case TRACK -> FieldKey.TRACK;
            case WORK -> FieldKey.WORK;
            case WORK_TYPE -> FieldKey.WORK_TYPE;
        };
    }
}
