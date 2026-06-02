package pl.khuzzuk.player;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.Header;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MP3Player implements SoundPlayer {
    private final Object lock = new Object();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mp3-player");
        thread.setDaemon(true);
        return thread;
    });
    private SoundFile currentSoundFile;
    private AdvancedPlayer currentPlayer;
    private VolumeAwareJavaSoundAudioDevice currentAudioDevice;
    private InputStream currentStream;
    private long playbackSession;
    private Mp3Info currentMp3Info;
    private int currentPositionMillis;
    private long playbackStartNanos;
    private boolean paused;
    private boolean stopped = true;
    private int volumePercent = 100;

    @Override
    public void play(SoundFile soundFile) {
        validate(soundFile);
        start(soundFile);
    }

    @Override
    public void pause() {
        synchronized (lock) {
            if (currentPlayer == null || stopped) {
                return;
            }

            currentPositionMillis = currentPositionMillisLocked();
            paused = true;
            closeCurrentPlayer();
        }
    }

    @Override
    public void resume() {
        SoundFile soundFile;
        Mp3Info mp3Info;
        int frame;
        synchronized (lock) {
            if (!paused || currentSoundFile == null) {
                return;
            }

            soundFile = currentSoundFile;
            mp3Info = currentMp3Info;
            frame = frameForMillis(mp3Info, currentPositionMillis);
        }

        start(soundFile, mp3Info, frame);
    }

    @Override
    public void stop() {
        synchronized (lock) {
            stopped = true;
            paused = false;
            currentPositionMillis = 0;
            playbackStartNanos = 0;
            currentSoundFile = null;
            currentMp3Info = null;
            closeCurrentPlayer();
        }
    }

    @Override
    public void seekToMillis(int positionMillis) {
        SoundFile soundFile;
        Mp3Info mp3Info;
        int frame;
        boolean shouldResume;
        synchronized (lock) {
            if (currentSoundFile == null || currentMp3Info == null) {
                return;
            }

            soundFile = currentSoundFile;
            mp3Info = currentMp3Info;
            currentPositionMillis = Math.clamp(positionMillis, 0, currentMp3Info.durationMillis());
            frame = frameForMillis(currentMp3Info, currentPositionMillis);
            shouldResume = !paused && !stopped;
            if (!shouldResume) {
                playbackStartNanos = 0;
                return;
            }
        }

        start(soundFile, mp3Info, frame);
    }

    @Override
    public int getCurrentPositionMillis() {
        synchronized (lock) {
            return currentPositionMillisLocked();
        }
    }

    @Override
    public int getCurrentDurationMillis() {
        synchronized (lock) {
            return currentMp3Info == null ? 0 : currentMp3Info.durationMillis();
        }
    }

    @Override
    public void setVolumePercent(int volumePercent) {
        synchronized (lock) {
            this.volumePercent = Math.clamp(volumePercent, 0, 100);
            if (currentAudioDevice != null) {
                currentAudioDevice.setVolumePercent(this.volumePercent);
            }
        }
    }

    private void start(SoundFile soundFile) {
        start(soundFile, readMp3Info(soundFile), 0);
    }

    private void start(SoundFile soundFile, Mp3Info mp3Info, int startFrame) {
        long session;
        synchronized (lock) {
            stopped = true;
            paused = false;
            closeCurrentPlayer();
            currentSoundFile = soundFile;
            currentMp3Info = mp3Info;
            currentPositionMillis = millisForFrame(mp3Info, startFrame);
            playbackStartNanos = System.nanoTime();
            stopped = false;
            session = ++playbackSession;
        }

        executorService.submit(() -> playFromFrame(soundFile, startFrame, session));
    }

    private void validate(SoundFile soundFile) {
        if (soundFile == null || soundFile.path() == null || soundFile.path().isBlank()) {
            throw new IllegalArgumentException("Sound file path is required");
        }
    }

    private Mp3Info readMp3Info(SoundFile soundFile) {
        Path path = Path.of(soundFile.path());

        try (InputStream stream = new BufferedInputStream(Files.newInputStream(path))) {
            Bitstream bitstream = new Bitstream(stream);
            Header header = bitstream.readFrame();
            if (header == null) {
                throw new IllegalStateException("Cannot read MP3 header: " + soundFile.path());
            }

            float millisPerFrame = header.ms_per_frame();
            int streamSize = (int) Math.min(Integer.MAX_VALUE, Files.size(path));
            int durationMillis = Math.max(0, Math.round(header.total_ms(streamSize)));
            return new Mp3Info(millisPerFrame, durationMillis);
        } catch (IOException | JavaLayerException e) {
            throw new IllegalStateException("Cannot read MP3 header: " + soundFile.path(), e);
        }
    }

    private int frameForMillis(Mp3Info mp3Info, int millis) {
        if (mp3Info == null || millis <= 0) {
            return 0;
        }

        return Math.max(0, Math.round(millis / mp3Info.millisPerFrame()));
    }

    private int millisForFrame(Mp3Info mp3Info, int frame) {
        if (mp3Info == null || frame <= 0) {
            return 0;
        }

        return Math.max(0, Math.round(frame * mp3Info.millisPerFrame()));
    }

    private int currentPositionMillisLocked() {
        if (stopped || paused || playbackStartNanos == 0) {
            return currentPositionMillis;
        }

        long elapsedMillis = (System.nanoTime() - playbackStartNanos) / 1_000_000;
        int position = Math.toIntExact(Math.min(Integer.MAX_VALUE, currentPositionMillis + elapsedMillis));
        if (currentMp3Info == null || currentMp3Info.durationMillis() <= 0) {
            return position;
        }

        return Math.min(position, currentMp3Info.durationMillis());
    }

    private void playFromFrame(SoundFile soundFile, int startFrame, long session) {
        try (InputStream stream = new BufferedInputStream(Files.newInputStream(Path.of(soundFile.path())))) {
            VolumeAwareJavaSoundAudioDevice audioDevice = new VolumeAwareJavaSoundAudioDevice(this::getVolumePercent);
            AdvancedPlayer player = new AdvancedPlayer(stream, audioDevice);
            player.setPlayBackListener(new CurrentFrameListener(startFrame, session));
            synchronized (lock) {
                if (session != playbackSession) {
                    player.close();
                    return;
                }

                currentStream = stream;
                currentPlayer = player;
                currentAudioDevice = audioDevice;
            }

            player.play(startFrame, Integer.MAX_VALUE);
            synchronized (lock) {
                if (session == playbackSession && !paused) {
                    stopped = true;
                    currentPositionMillis = currentMp3Info == null ? 0 : currentMp3Info.durationMillis();
                    playbackStartNanos = 0;
                }
            }
        } catch (IOException | JavaLayerException e) {
            boolean expectedClose;
            synchronized (lock) {
                expectedClose = session != playbackSession || paused || stopped;
                if (!expectedClose) {
                    stopped = true;
                    paused = false;
                    currentPositionMillis = 0;
                    playbackStartNanos = 0;
                }
            }
            if (!expectedClose) {
                throw new IllegalStateException("Cannot play MP3 file: " + soundFile.path(), e);
            }
        } finally {
            synchronized (lock) {
                if (session == playbackSession) {
                    currentPlayer = null;
                    currentAudioDevice = null;
                    currentStream = null;
                }
            }
        }
    }

    private void closeCurrentPlayer() {
        if (currentPlayer != null) {
            currentPlayer.close();
        }
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException ignored) {
                // Closing playback is best effort.
            }
        }
    }

    private int getVolumePercent() {
        synchronized (lock) {
            return volumePercent;
        }
    }

    private record Mp3Info(float millisPerFrame, int durationMillis) {
    }

    private class CurrentFrameListener extends PlaybackListener {
        private final int startFrame;
        private final long session;

        private CurrentFrameListener(int startFrame, long session) {
            this.startFrame = startFrame;
            this.session = session;
        }

        @Override
        public void playbackFinished(PlaybackEvent event) {
            synchronized (lock) {
                if (session == playbackSession && paused) {
                    currentPositionMillis = millisForFrame(currentMp3Info, startFrame) + Math.max(0, event.getFrame());
                    playbackStartNanos = 0;
                }
            }
        }
    }
}
