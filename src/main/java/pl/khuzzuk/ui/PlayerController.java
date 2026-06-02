package pl.khuzzuk.ui;

import pl.khuzzuk.player.SoundFile;
import pl.khuzzuk.player.SoundPlayer;

import javax.swing.JOptionPane;
import java.awt.Component;

public class PlayerController {
    private final SoundPlayer soundPlayer;
    private final PlaylistPane playlistPane;
    private SoundFile activeSoundFile;
    private boolean playing;
    private boolean paused;

    public PlayerController(SoundPlayer soundPlayer, PlaylistPane playlistPane) {
        this.soundPlayer = soundPlayer;
        this.playlistPane = playlistPane;
    }

    public boolean playPause(Component parent) {
        if (playing) {
            soundPlayer.pause();
            playing = false;
            paused = true;
            return false;
        }

        SoundFile currentSoundFile = playlistPane.getCurrentSoundFile();
        if (currentSoundFile == null) {
            return false;
        }

        try {
            if (paused && currentSoundFile == activeSoundFile) {
                soundPlayer.resume();
            } else {
                soundPlayer.play(currentSoundFile);
                activeSoundFile = currentSoundFile;
            }
            playing = true;
            paused = false;
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            playing = false;
            paused = false;
            JOptionPane.showMessageDialog(
                    parent,
                    "Nie udalo sie odtworzyc utworu.",
                    "Blad odtwarzania",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public int getCurrentPositionMillis() {
        return soundPlayer.getCurrentPositionMillis();
    }

    public int getCurrentDurationMillis() {
        return soundPlayer.getCurrentDurationMillis();
    }

    public boolean seekToMillis(int positionMillis) {
        soundPlayer.seekToMillis(positionMillis);
        return isPlaying();
    }

    public void setVolumePercent(int volumePercent) {
        soundPlayer.setVolumePercent(volumePercent);
    }

    public boolean isPlaying() {
        int durationMillis = soundPlayer.getCurrentDurationMillis();
        if (playing && durationMillis > 0 && soundPlayer.getCurrentPositionMillis() >= durationMillis) {
            return playNextTrack();
        }

        return playing;
    }

    public boolean playNext() {
        return playSoundFile(playlistPane.moveToNextSoundFile());
    }

    public boolean playPrevious() {
        return playSoundFile(playlistPane.moveToPreviousSoundFile());
    }

    public void stop() {
        soundPlayer.stop();
        playing = false;
        paused = false;
    }

    private boolean playNextTrack() {
        return playSoundFile(playlistPane.moveToNextSoundFile());
    }

    private boolean playSoundFile(SoundFile soundFile) {
        if (soundFile == null) {
            playing = false;
            paused = false;
            activeSoundFile = null;
            return false;
        }

        try {
            soundPlayer.play(soundFile);
            activeSoundFile = soundFile;
            playing = true;
            paused = false;
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            playing = false;
            paused = false;
            return false;
        }
    }
}
