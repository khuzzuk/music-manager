package pl.khuzzuk.player;

import org.jflac.FLACDecoder;
import org.jflac.frame.Frame;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FLACPlayer implements SoundPlayer {
    private final Object lock = new Object();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "flac-player");
        thread.setDaemon(true);
        return thread;
    });
    private SoundFile currentSoundFile;
    private SourceDataLine currentLine;
    private InputStream currentStream;
    private FlacInfo currentFlacInfo;
    private long playbackSession;
    private int currentPositionMillis;
    private long playbackStartNanos;
    private boolean paused;
    private boolean stopped = true;
    private int volumePercent = 100;

    @Override
    public void play(SoundFile soundFile) {
        validate(soundFile);
        start(soundFile, readFlacInfo(soundFile), 0);
    }

    @Override
    public void pause() {
        synchronized (lock) {
            if (currentLine == null || stopped) {
                return;
            }

            currentPositionMillis = currentPositionMillisLocked();
            paused = true;
            closeCurrentPlayback();
        }
    }

    @Override
    public void resume() {
        SoundFile soundFile;
        FlacInfo flacInfo;
        int positionMillis;
        synchronized (lock) {
            if (!paused || currentSoundFile == null) {
                return;
            }

            soundFile = currentSoundFile;
            flacInfo = currentFlacInfo;
            positionMillis = currentPositionMillis;
        }

        start(soundFile, flacInfo, positionMillis);
    }

    @Override
    public void stop() {
        synchronized (lock) {
            stopped = true;
            paused = false;
            currentPositionMillis = 0;
            playbackStartNanos = 0;
            currentSoundFile = null;
            currentFlacInfo = null;
            closeCurrentPlayback();
        }
    }

    @Override
    public void seekToMillis(int positionMillis) {
        SoundFile soundFile;
        FlacInfo flacInfo;
        int targetPositionMillis;
        boolean shouldResume;
        synchronized (lock) {
            if (currentSoundFile == null || currentFlacInfo == null) {
                return;
            }

            soundFile = currentSoundFile;
            flacInfo = currentFlacInfo;
            targetPositionMillis = Math.clamp(positionMillis, 0, currentFlacInfo.durationMillis());
            currentPositionMillis = targetPositionMillis;
            shouldResume = !paused && !stopped;
            if (!shouldResume) {
                playbackStartNanos = 0;
                return;
            }
        }

        start(soundFile, flacInfo, targetPositionMillis);
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
            return currentFlacInfo == null ? 0 : currentFlacInfo.durationMillis();
        }
    }

    @Override
    public void setVolumePercent(int volumePercent) {
        synchronized (lock) {
            this.volumePercent = Math.clamp(volumePercent, 0, 100);
            AudioLineVolume.setVolumePercent(currentLine, this.volumePercent);
        }
    }

    private void start(SoundFile soundFile, FlacInfo flacInfo, int positionMillis) {
        long session;
        int boundedPositionMillis = Math.clamp(positionMillis, 0, flacInfo.durationMillis());
        synchronized (lock) {
            stopped = true;
            paused = false;
            closeCurrentPlayback();
            currentSoundFile = soundFile;
            currentFlacInfo = flacInfo;
            currentPositionMillis = boundedPositionMillis;
            playbackStartNanos = System.nanoTime();
            stopped = false;
            session = ++playbackSession;
        }

        executorService.submit(() -> playFromMillis(soundFile, flacInfo, boundedPositionMillis, session));
    }

    private void playFromMillis(SoundFile soundFile, FlacInfo flacInfo, int positionMillis, long session) {
        try (InputStream stream = openFlacStream(soundFile)) {
            FLACDecoder decoder = new FLACDecoder(stream);
            StreamInfo streamInfo = decoder.readStreamInfo();
            decoder.readMetadata(streamInfo);
            SourceDataLine line = openLine(flacInfo.format());
            synchronized (lock) {
                if (session != playbackSession) {
                    line.close();
                    return;
                }

                AudioLineVolume.setVolumePercent(line, volumePercent);
                currentStream = stream;
                currentLine = line;
            }

            line.start();
            long bytesToSkip = bytesForMillis(flacInfo, positionMillis);
            ByteData pcmData = null;
            Frame frame;
            while ((frame = decoder.readNextFrame()) != null) {
                synchronized (lock) {
                    if (session != playbackSession || paused || stopped || currentLine == null) {
                        break;
                    }
                }

                pcmData = decoder.decodeFrame(frame, pcmData);
                int offset = 0;
                int length = pcmData.getLen();
                if (bytesToSkip > 0) {
                    int skipped = Math.toIntExact(Math.min(bytesToSkip, length));
                    bytesToSkip -= skipped;
                    offset += skipped;
                    length -= skipped;
                }

                if (length > 0) {
                    line.write(pcmData.getData(), offset, length);
                }
            }

            boolean completedNormally;
            synchronized (lock) {
                completedNormally = session == playbackSession && !paused && !stopped;
            }
            if (completedNormally) {
                line.drain();
            }

            synchronized (lock) {
                if (completedNormally) {
                    stopped = true;
                    currentPositionMillis = currentFlacInfo == null ? 0 : currentFlacInfo.durationMillis();
                    playbackStartNanos = 0;
                }
            }
        } catch (IOException | LineUnavailableException e) {
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
                throw new IllegalStateException("Cannot play FLAC file: " + soundFile.path(), e);
            }
        } finally {
            synchronized (lock) {
                if (session == playbackSession) {
                    currentLine = null;
                    currentStream = null;
                }
            }
        }
    }

    private FlacInfo readFlacInfo(SoundFile soundFile) {
        try (InputStream stream = openFlacStream(soundFile)) {
            FLACDecoder decoder = new FLACDecoder(stream);
            StreamInfo streamInfo = decoder.readStreamInfo();
            AudioFormat format = streamInfo.getAudioFormat();
            return new FlacInfo(format, durationMillis(streamInfo));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read FLAC file: " + soundFile.path(), e);
        }
    }

    private int durationMillis(StreamInfo streamInfo) {
        long totalSamples = streamInfo.getTotalSamples();
        int sampleRate = streamInfo.getSampleRate();
        if (totalSamples <= 0 || sampleRate <= 0) {
            return 0;
        }

        return Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.round(totalSamples * 1000.0 / sampleRate)));
    }

    private InputStream openFlacStream(SoundFile soundFile) throws IOException {
        return new BufferedInputStream(Files.newInputStream(Path.of(soundFile.path())));
    }

    private SourceDataLine openLine(AudioFormat format) throws LineUnavailableException {
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
        line.open(format);
        return line;
    }

    private long bytesForMillis(FlacInfo flacInfo, int positionMillis) {
        long targetFrames = Math.round(positionMillis * flacInfo.format().getFrameRate() / 1000.0);
        return targetFrames * flacInfo.format().getFrameSize();
    }

    private int currentPositionMillisLocked() {
        if (stopped || paused || playbackStartNanos == 0) {
            return currentPositionMillis;
        }

        long elapsedMillis = (System.nanoTime() - playbackStartNanos) / 1_000_000;
        int position = Math.toIntExact(Math.min(Integer.MAX_VALUE, currentPositionMillis + elapsedMillis));
        if (currentFlacInfo == null || currentFlacInfo.durationMillis() <= 0) {
            return position;
        }

        return Math.min(position, currentFlacInfo.durationMillis());
    }

    private void closeCurrentPlayback() {
        if (currentLine != null) {
            currentLine.stop();
            currentLine.close();
        }
        if (currentStream != null) {
            try {
                currentStream.close();
            } catch (IOException ignored) {
                // Closing playback is best effort.
            }
        }
    }

    private void validate(SoundFile soundFile) {
        if (soundFile == null || soundFile.path() == null || soundFile.path().isBlank()) {
            throw new IllegalArgumentException("Sound file path is required");
        }
    }

    private record FlacInfo(AudioFormat format, int durationMillis) {
    }
}
