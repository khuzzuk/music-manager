package pl.khuzzuk.player;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WAVPlayer implements SoundPlayer {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "wav-player");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();
    private SoundFile currentSoundFile;
    private WavInfo currentWavInfo;
    private SourceDataLine currentLine;
    private int currentPositionMillis;
    private long playbackStartNanos;
    private long playbackSession;
    private boolean paused;
    private int volumePercent = 100;

    @Override
    public void play(SoundFile soundFile) {
        validate(soundFile);
        start(soundFile, readWavInfo(soundFile), 0);
    }

    @Override
    public void pause() {
        synchronized (lock) {
            if (currentSoundFile == null || paused) {
                return;
            }

            currentPositionMillis = getCurrentPositionMillis();
            paused = true;
            closeCurrentLine();
        }
    }

    @Override
    public void resume() {
        SoundFile soundFile;
        WavInfo wavInfo;
        int positionMillis;
        synchronized (lock) {
            if (currentSoundFile == null || currentWavInfo == null || !paused) {
                return;
            }

            soundFile = currentSoundFile;
            wavInfo = currentWavInfo;
            positionMillis = currentPositionMillis;
        }

        start(soundFile, wavInfo, positionMillis);
    }

    @Override
    public void stop() {
        synchronized (lock) {
            playbackSession++;
            currentSoundFile = null;
            currentWavInfo = null;
            currentPositionMillis = 0;
            paused = false;
            closeCurrentLine();
        }
    }

    @Override
    public void seekToMillis(int positionMillis) {
        SoundFile soundFile;
        WavInfo wavInfo;
        int targetPositionMillis;
        boolean shouldRestart;
        synchronized (lock) {
            if (currentSoundFile == null || currentWavInfo == null) {
                return;
            }

            soundFile = currentSoundFile;
            wavInfo = currentWavInfo;
            targetPositionMillis = Math.clamp(positionMillis, 0, currentWavInfo.durationMillis());
            currentPositionMillis = targetPositionMillis;
            shouldRestart = !paused;
            closeCurrentLine();
        }

        if (shouldRestart) {
            start(soundFile, wavInfo, targetPositionMillis);
        }
    }

    @Override
    public int getCurrentPositionMillis() {
        synchronized (lock) {
            if (currentSoundFile == null || paused) {
                return currentPositionMillis;
            }

            long elapsedMillis = (System.nanoTime() - playbackStartNanos) / 1_000_000L;
            int position = Math.toIntExact(Math.min(Integer.MAX_VALUE, currentPositionMillis + elapsedMillis));
            return currentWavInfo == null ? position : Math.min(position, currentWavInfo.durationMillis());
        }
    }

    @Override
    public int getCurrentDurationMillis() {
        synchronized (lock) {
            return currentWavInfo == null ? 0 : currentWavInfo.durationMillis();
        }
    }

    @Override
    public void setVolumePercent(int volumePercent) {
        synchronized (lock) {
            this.volumePercent = Math.clamp(volumePercent, 0, 100);
            AudioLineVolume.setVolumePercent(currentLine, this.volumePercent);
        }
    }

    private void start(SoundFile soundFile, WavInfo wavInfo, int positionMillis) {
        int boundedPositionMillis = Math.clamp(positionMillis, 0, wavInfo.durationMillis());
        long session;
        synchronized (lock) {
            playbackSession++;
            session = playbackSession;
            closeCurrentLine();
            currentSoundFile = soundFile;
            currentWavInfo = wavInfo;
            currentPositionMillis = boundedPositionMillis;
            playbackStartNanos = System.nanoTime();
            paused = false;
        }

        executorService.submit(() -> playFromMillis(soundFile, wavInfo, boundedPositionMillis, session));
    }

    private void playFromMillis(SoundFile soundFile, WavInfo wavInfo, int positionMillis, long session) {
        try (AudioInputStream stream = openWavStream(soundFile)) {
            skipToMillis(stream, wavInfo, positionMillis);
            SourceDataLine line = openLine(wavInfo.format());
            synchronized (lock) {
                if (session != playbackSession) {
                    line.close();
                    return;
                }
                AudioLineVolume.setVolumePercent(line, volumePercent);
                currentLine = line;
                playbackStartNanos = System.nanoTime();
            }

            line.start();
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                synchronized (lock) {
                    if (session != playbackSession || currentLine != line) {
                        return;
                    }
                }
                line.write(buffer, 0, read);
            }
            line.drain();
            finishPlayback(session);
        } catch (IOException | UnsupportedAudioFileException e) {
            if (isCurrentSession(session)) {
                throw new IllegalStateException("Cannot play WAV file: " + soundFile.path(), e);
            }
        }
    }

    private void finishPlayback(long session) {
        synchronized (lock) {
            if (session != playbackSession || currentSoundFile == null) {
                return;
            }

            currentPositionMillis = currentWavInfo == null ? 0 : currentWavInfo.durationMillis();
            currentLine = null;
            paused = false;
        }
    }

    private boolean isCurrentSession(long session) {
        synchronized (lock) {
            return session == playbackSession;
        }
    }

    private WavInfo readWavInfo(SoundFile soundFile) {
        try (AudioInputStream stream = openWavStream(soundFile)) {
            AudioFormat format = stream.getFormat();
            return new WavInfo(format, durationMillis(format, stream.getFrameLength()));
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new IllegalStateException("Cannot read WAV file: " + soundFile.path(), e);
        }
    }

    private AudioInputStream openWavStream(SoundFile soundFile) throws IOException, UnsupportedAudioFileException {
        return AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Path.of(soundFile.path()))));
    }

    private SourceDataLine openLine(AudioFormat format) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            return line;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open WAV audio line.", e);
        }
    }

    private void skipToMillis(AudioInputStream stream, WavInfo wavInfo, int positionMillis) throws IOException {
        long bytesToSkip = bytesForMillis(wavInfo, positionMillis);
        while (bytesToSkip > 0) {
            long skipped = stream.skip(bytesToSkip);
            if (skipped <= 0) {
                return;
            }
            bytesToSkip -= skipped;
        }
    }

    private long bytesForMillis(WavInfo wavInfo, int positionMillis) {
        long targetFrames = Math.round(positionMillis * wavInfo.format().getFrameRate() / 1000.0);
        return targetFrames * wavInfo.format().getFrameSize();
    }

    private int durationMillis(AudioFormat format, long frameLength) {
        if (frameLength <= 0 || format.getFrameRate() <= 0) {
            return 0;
        }

        return Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.round(frameLength * 1000.0 / format.getFrameRate())));
    }

    private void closeCurrentLine() {
        if (currentLine != null) {
            currentLine.stop();
            currentLine.close();
            currentLine = null;
        }
    }

    private void validate(SoundFile soundFile) {
        if (soundFile == null || soundFile.path() == null || soundFile.path().isBlank()) {
            throw new IllegalArgumentException("Sound file path is required.");
        }
    }

    private record WavInfo(AudioFormat format, int durationMillis) {
    }
}
