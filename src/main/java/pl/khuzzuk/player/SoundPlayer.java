package pl.khuzzuk.player;

public interface SoundPlayer {
    void play(SoundFile soundFile);

    void pause();

    void resume();

    void stop();

    void seekToMillis(int positionMillis);

    void setVolumePercent(int volumePercent);

    int getCurrentPositionMillis();

    int getCurrentDurationMillis();
}
