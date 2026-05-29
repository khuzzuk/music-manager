package pl.khuzzuk.player;

import java.nio.file.Path;

public class SoundPlayerRouter implements SoundPlayer {
    private final SoundPlayer mp3Player;
    private final SoundPlayer flacPlayer;
    private SoundPlayer currentPlayer;

    public SoundPlayerRouter(SoundPlayer mp3Player, SoundPlayer flacPlayer) {
        this.mp3Player = mp3Player;
        this.flacPlayer = flacPlayer;
    }

    @Override
    public void play(SoundFile soundFile) {
        SoundPlayer player = playerFor(soundFile);
        stopCurrentIfDifferent(player);
        currentPlayer = player;
        player.play(soundFile);
    }

    @Override
    public void pause() {
        if (currentPlayer != null) {
            currentPlayer.pause();
        }
    }

    @Override
    public void resume() {
        if (currentPlayer != null) {
            currentPlayer.resume();
        }
    }

    @Override
    public void stop() {
        if (currentPlayer != null) {
            currentPlayer.stop();
        }
    }

    @Override
    public void seekToMillis(int positionMillis) {
        if (currentPlayer != null) {
            currentPlayer.seekToMillis(positionMillis);
        }
    }

    @Override
    public int getCurrentPositionMillis() {
        return currentPlayer == null ? 0 : currentPlayer.getCurrentPositionMillis();
    }

    @Override
    public int getCurrentDurationMillis() {
        return currentPlayer == null ? 0 : currentPlayer.getCurrentDurationMillis();
    }

    private SoundPlayer playerFor(SoundFile soundFile) {
        SoundFileType soundFileType = SoundFileType.fromPath(Path.of(soundFile.path()))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sound file: " + soundFile.path()));
        return switch (soundFileType) {
            case MP3 -> mp3Player;
            case FLAC -> flacPlayer;
        };
    }

    private void stopCurrentIfDifferent(SoundPlayer player) {
        if (currentPlayer != null && currentPlayer != player) {
            currentPlayer.stop();
        }
    }
}
