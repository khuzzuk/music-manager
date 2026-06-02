package pl.khuzzuk.player;

import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDeviceBase;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.function.IntSupplier;

class VolumeAwareJavaSoundAudioDevice extends AudioDeviceBase {
    private final IntSupplier volumePercentSupplier;
    private final Object lineLock = new Object();
    private SourceDataLine source;
    private AudioFormat format;
    private byte[] byteBuffer = new byte[4096];

    VolumeAwareJavaSoundAudioDevice(IntSupplier volumePercentSupplier) {
        this.volumePercentSupplier = volumePercentSupplier;
    }

    @Override
    protected void closeImpl() {
        synchronized (lineLock) {
            if (source != null) {
                source.close();
                source = null;
            }
        }
    }

    @Override
    protected void writeImpl(short[] samples, int offset, int length) throws JavaLayerException {
        SourceDataLine line = source;
        if (line == null) {
            createSource();
            line = source;
        }

        byte[] bytes = toByteArray(samples, offset, length);
        if (line != null) {
            line.write(bytes, 0, length * 2);
        }
    }

    @Override
    protected void flushImpl() {
        SourceDataLine line = source;
        if (line != null) {
            line.drain();
        }
    }

    @Override
    public int getPosition() {
        SourceDataLine line = source;
        return line == null ? 0 : (int) (line.getMicrosecondPosition() / 1000);
    }

    void setVolumePercent(int volumePercent) {
        synchronized (lineLock) {
            AudioLineVolume.setVolumePercent(source, volumePercent);
        }
    }

    private void createSource() throws JavaLayerException {
        Throwable failure = null;
        try {
            Line line = AudioSystem.getLine(getSourceLineInfo());
            if (line instanceof SourceDataLine sourceDataLine) {
                synchronized (lineLock) {
                    source = sourceDataLine;
                    source.open(getAudioFormat());
                    AudioLineVolume.setVolumePercent(source, volumePercentSupplier.getAsInt());
                    source.start();
                }
            }
        } catch (RuntimeException | LinkageError | LineUnavailableException e) {
            failure = e;
        }

        if (source == null) {
            throw new JavaLayerException("Cannot obtain source audio line", failure);
        }
    }

    private DataLine.Info getSourceLineInfo() {
        return new DataLine.Info(SourceDataLine.class, getAudioFormat());
    }

    private AudioFormat getAudioFormat() {
        if (format == null) {
            Decoder decoder = getDecoder();
            format = new AudioFormat(decoder.getOutputFrequency(), 16, decoder.getOutputChannels(), true, false);
        }
        return format;
    }

    private byte[] toByteArray(short[] samples, int offset, int length) {
        byte[] bytes = getByteArray(length * 2);
        int index = 0;
        while (length-- > 0) {
            short sample = samples[offset++];
            bytes[index++] = (byte) sample;
            bytes[index++] = (byte) (sample >>> 8);
        }
        return bytes;
    }

    private byte[] getByteArray(int length) {
        if (byteBuffer.length < length) {
            byteBuffer = new byte[length + 1024];
        }
        return byteBuffer;
    }
}
