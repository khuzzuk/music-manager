package pl.khuzzuk.player;

public interface SoundPlayer {
    void play(SoundFile soundFile);

    void pause();

    void resume();

    void stop();

    void seekToMillis(int positionMillis);

    int getCurrentPositionMillis();

    int getCurrentDurationMillis();
}
