package pl.khuzzuk.metadata;

import pl.khuzzuk.player.SoundFileType;

import java.nio.file.Path;
import java.util.Objects;

public class SoundFileMetadata {
    private SoundFileType format;
    private String path;
    private String fileName;
    private String indexedPath;
    private String title;
    private int rating;
    private String date;
    private String artist;
    private String artists;
    private String album;
    private String albumArtist;
    private String albumArtists;
    private String composer;
    private String conductor;
    private String country;
    private String custom1;
    private String custom2;
    private String custom3;
    private String custom4;
    private String custom5;
    private String discNo;
    private String genre;
    private String group;
    private String instrument;
    private String mood;
    private String movement;
    private String occasion;
    private String opus;
    private String orchestra;
    private String quality;
    private String ranking;
    private String tempo;
    private String tonality;
    private String track;
    private String work;
    private String workType;

    public SoundFileMetadata(
            SoundFileType format,
            String path,
            String fileName,
            String indexedPath,
            String title,
            int rating,
            String date,
            String artist,
            String artists,
            String album,
            String albumArtist,
            String albumArtists,
            String composer,
            String conductor,
            String country,
            String custom1,
            String custom2,
            String custom3,
            String custom4,
            String custom5,
            String discNo,
            String genre,
            String group,
            String instrument,
            String mood,
            String movement,
            String occasion,
            String opus,
            String orchestra,
            String quality,
            String ranking,
            String tempo,
            String tonality,
            String track,
            String work,
            String workType) {
        this.format = Objects.requireNonNull(format, "format");
        this.path = path;
        this.fileName = fileName;
        this.indexedPath = indexedPath;
        this.title = title;
        this.rating = rating;
        this.date = date;
        this.artist = artist;
        this.artists = artists;
        this.album = album;
        this.albumArtist = albumArtist;
        this.albumArtists = albumArtists;
        this.composer = composer;
        this.conductor = conductor;
        this.country = country;
        this.custom1 = custom1;
        this.custom2 = custom2;
        this.custom3 = custom3;
        this.custom4 = custom4;
        this.custom5 = custom5;
        this.discNo = discNo;
        this.genre = genre;
        this.group = group;
        this.instrument = instrument;
        this.mood = mood;
        this.movement = movement;
        this.occasion = occasion;
        this.opus = opus;
        this.orchestra = orchestra;
        this.quality = quality;
        this.ranking = ranking;
        this.tempo = tempo;
        this.tonality = tonality;
        this.track = track;
        this.work = work;
        this.workType = workType;
    }

    public static SoundFileMetadata empty(Path path) {
        return empty(path, null);
    }

    public static SoundFileMetadata empty(Path path, Path indexedPath) {
        SoundFileType format = SoundFileType.fromPath(path)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sound file path: " + path));
        return new SoundFileMetadata(
                format,
                path.toAbsolutePath().normalize().toString(),
                path.getFileName().toString(),
                indexedPath == null ? null : indexedPath.toAbsolutePath().normalize().toString(),
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public SoundFileType getFormat() {
        return format;
    }

    public void setFormat(SoundFileType format) {
        this.format = Objects.requireNonNull(format, "format");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getIndexedPath() {
        return indexedPath;
    }

    public void setIndexedPath(String indexedPath) {
        this.indexedPath = indexedPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    public String getAlbumArtists() {
        return albumArtists;
    }

    public void setAlbumArtists(String albumArtists) {
        this.albumArtists = albumArtists;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getConductor() {
        return conductor;
    }

    public void setConductor(String conductor) {
        this.conductor = conductor;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCustom1() {
        return custom1;
    }

    public void setCustom1(String custom1) {
        this.custom1 = custom1;
    }

    public String getCustom2() {
        return custom2;
    }

    public void setCustom2(String custom2) {
        this.custom2 = custom2;
    }

    public String getCustom3() {
        return custom3;
    }

    public void setCustom3(String custom3) {
        this.custom3 = custom3;
    }

    public String getCustom4() {
        return custom4;
    }

    public void setCustom4(String custom4) {
        this.custom4 = custom4;
    }

    public String getCustom5() {
        return custom5;
    }

    public void setCustom5(String custom5) {
        this.custom5 = custom5;
    }

    public String getDiscNo() {
        return discNo;
    }

    public void setDiscNo(String discNo) {
        this.discNo = discNo;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getMovement() {
        return movement;
    }

    public void setMovement(String movement) {
        this.movement = movement;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public String getOpus() {
        return opus;
    }

    public void setOpus(String opus) {
        this.opus = opus;
    }

    public String getOrchestra() {
        return orchestra;
    }

    public void setOrchestra(String orchestra) {
        this.orchestra = orchestra;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getRanking() {
        return ranking;
    }

    public void setRanking(String ranking) {
        this.ranking = ranking;
    }

    public String getTempo() {
        return tempo;
    }

    public void setTempo(String tempo) {
        this.tempo = tempo;
    }

    public String getTonality() {
        return tonality;
    }

    public void setTonality(String tonality) {
        this.tonality = tonality;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public SoundFileType format() {
        return getFormat();
    }

    public String path() {
        return getPath();
    }

    public String fileName() {
        return getFileName();
    }

    public String indexedPath() {
        return getIndexedPath();
    }

    public String title() {
        return getTitle();
    }

    public int rating() {
        return getRating();
    }

    public String date() {
        return getDate();
    }

    public String artist() {
        return getArtist();
    }

    public String artists() {
        return getArtists();
    }

    public String album() {
        return getAlbum();
    }

    public String albumArtist() {
        return getAlbumArtist();
    }

    public String albumArtists() {
        return getAlbumArtists();
    }

    public String composer() {
        return getComposer();
    }

    public String conductor() {
        return getConductor();
    }

    public String country() {
        return getCountry();
    }

    public String custom1() {
        return getCustom1();
    }

    public String custom2() {
        return getCustom2();
    }

    public String custom3() {
        return getCustom3();
    }

    public String custom4() {
        return getCustom4();
    }

    public String custom5() {
        return getCustom5();
    }

    public String discNo() {
        return getDiscNo();
    }

    public String genre() {
        return getGenre();
    }

    public String group() {
        return getGroup();
    }

    public String instrument() {
        return getInstrument();
    }

    public String mood() {
        return getMood();
    }

    public String movement() {
        return getMovement();
    }

    public String occasion() {
        return getOccasion();
    }

    public String opus() {
        return getOpus();
    }

    public String orchestra() {
        return getOrchestra();
    }

    public String quality() {
        return getQuality();
    }

    public String ranking() {
        return getRanking();
    }

    public String tempo() {
        return getTempo();
    }

    public String tonality() {
        return getTonality();
    }

    public String track() {
        return getTrack();
    }

    public String work() {
        return getWork();
    }

    public String workType() {
        return getWorkType();
    }
}
