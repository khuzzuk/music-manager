package pl.khuzzuk.metadata;

import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractTagFrame;
import org.jaudiotagger.tag.id3.AbstractTagFrameBody;
import org.jaudiotagger.tag.id3.ID3v23Frame;

public class MoodConverter {
    public String getMood(Tag tag) {
        if (tag == null) {
            return null;
        }

        String mood = tag.getFirst(FieldKey.MOOD);
        if (mood != null && !mood.isBlank()) {
            return mood;
        }

        return getFromComment(tag);
    }

    public String getFromComment(Tag tag) {
        if (tag == null) {
            return null;
        }

        return tag.getFields(FieldKey.COMMENT).stream()
                .filter(rawTag -> rawTag instanceof ID3v23Frame)
                .map(ID3v23Frame.class::cast)
                .map(AbstractTagFrame::getBody)
                .filter(body -> body.getBriefDescription().contains("MusicMatch_Mood"))
                .map(AbstractTagFrameBody::getUserFriendlyValue)
                .findAny().orElse(null);
    }
}
