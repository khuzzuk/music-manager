package pl.khuzzuk.player;

import javax.sound.sampled.AudioFileFormat;
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

public class OGGPlayer implements SoundPlayer {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ogg-player");
        thread.setDaemon(true);
        return thread;
    });
    private final Object lock = new Object();
    private SoundFile currentSoundFile;
    private OggInfo currentOggInfo;
    private SourceDataLine currentLine;
    private int currentPositionMillis;
    private long playbackStartNanos;
    private long playbackSession;
    private boolean paused;

    @Override
    public void play(SoundFile soundFile) {
        validate(soundFile);
        start(soundFile, readOggInfo(soundFile), 0);
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
        OggInfo oggInfo;
        int positionMillis;
        synchronized (lock) {
            if (currentSoundFile == null || currentOggInfo == null || !paused) {
                return;
            }

            soundFile = currentSoundFile;
            oggInfo = currentOggInfo;
            positionMillis = currentPositionMillis;
        }

        start(soundFile, oggInfo, positionMillis);
    }

    @Override
    public void stop() {
        synchronized (lock) {
            playbackSession++;
            currentSoundFile = null;
            currentOggInfo = null;
            currentPositionMillis = 0;
            paused = false;
            closeCurrentLine();
        }
    }

    @Override
    public void seekToMillis(int positionMillis) {
        SoundFile soundFile;
        OggInfo oggInfo;
        int targetPositionMillis;
        boolean shouldRestart;
        synchronized (lock) {
            if (currentSoundFile == null || currentOggInfo == null) {
                return;
            }

            soundFile = currentSoundFile;
            oggInfo = currentOggInfo;
            targetPositionMillis = Math.clamp(positionMillis, 0, currentOggInfo.durationMillis());
            currentPositionMillis = targetPositionMillis;
            shouldRestart = !paused;
            closeCurrentLine();
        }

        if (shouldRestart) {
            start(soundFile, oggInfo, targetPositionMillis);
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
            return currentOggInfo == null ? position : Math.min(position, currentOggInfo.durationMillis());
        }
    }

    @Override
    public int getCurrentDurationMillis() {
        synchronized (lock) {
            return currentOggInfo == null ? 0 : currentOggInfo.durationMillis();
        }
    }

    private void start(SoundFile soundFile, OggInfo oggInfo, int positionMillis) {
        int boundedPositionMillis = Math.clamp(positionMillis, 0, oggInfo.durationMillis());
        long session;
        synchronized (lock) {
            playbackSession++;
            session = playbackSession;
            closeCurrentLine();
            currentSoundFile = soundFile;
            currentOggInfo = oggInfo;
            currentPositionMillis = boundedPositionMillis;
            playbackStartNanos = System.nanoTime();
            paused = false;
        }

        executorService.submit(() -> playFromMillis(soundFile, oggInfo, boundedPositionMillis, session));
    }

    private void playFromMillis(SoundFile soundFile, OggInfo oggInfo, int positionMillis, long session) {
        try (AudioInputStream stream = openDecodedStream(soundFile)) {
            skipToMillis(stream, oggInfo, positionMillis);
            SourceDataLine line = openLine(oggInfo.format());
            synchronized (lock) {
                if (session != playbackSession) {
                    line.close();
                    return;
                }
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
                throw new IllegalStateException("Cannot play OGG file: " + soundFile.path(), e);
            }
        }
    }

    private void finishPlayback(long session) {
        synchronized (lock) {
            if (session != playbackSession || currentSoundFile == null) {
                return;
            }

            currentPositionMillis = currentOggInfo == null ? 0 : currentOggInfo.durationMillis();
            currentLine = null;
            paused = false;
        }
    }

    private boolean isCurrentSession(long session) {
        synchronized (lock) {
            return session == playbackSession;
        }
    }

    private OggInfo readOggInfo(SoundFile soundFile) {
        Path path = Path.of(soundFile.path());
        try (AudioInputStream stream = openDecodedStream(soundFile)) {
            AudioFormat format = stream.getFormat();
            return new OggInfo(format, durationMillis(path, format, stream.getFrameLength()));
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new IllegalStateException("Cannot read OGG file: " + soundFile.path(), e);
        }
    }

    private AudioInputStream openDecodedStream(SoundFile soundFile) throws IOException, UnsupportedAudioFileException {
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(
                new BufferedInputStream(Files.newInputStream(Path.of(soundFile.path()))));
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                16,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * 2,
                sourceFormat.getSampleRate(),
                false);
        try {
            return AudioSystem.getAudioInputStream(decodedFormat, sourceStream);
        } catch (IllegalArgumentException e) {
            sourceStream.close();
            throw e;
        }
    }

    private SourceDataLine openLine(AudioFormat format) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            return line;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open OGG audio line.", e);
        }
    }

    private void skipToMillis(AudioInputStream stream, OggInfo oggInfo, int positionMillis) throws IOException {
        long bytesToSkip = bytesForMillis(oggInfo, positionMillis);
        while (bytesToSkip > 0) {
            long skipped = stream.skip(bytesToSkip);
            if (skipped <= 0) {
                return;
            }
            bytesToSkip -= skipped;
        }
    }

    private long bytesForMillis(OggInfo oggInfo, int positionMillis) {
        long targetFrames = Math.round(positionMillis * oggInfo.format().getFrameRate() / 1000.0);
        return targetFrames * oggInfo.format().getFrameSize();
    }

    private int durationMillis(Path path, AudioFormat format, long frameLength) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(path.toFile());
        Object durationMicros = fileFormat.properties().get("duration");
        if (durationMicros instanceof Number number && number.longValue() > 0) {
            return Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.round(number.longValue() / 1000.0)));
        }

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

    private record OggInfo(AudioFormat format, int durationMillis) {
    }
}
