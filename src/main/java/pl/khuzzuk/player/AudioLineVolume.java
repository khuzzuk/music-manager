package pl.khuzzuk.player;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

class AudioLineVolume {
    private AudioLineVolume() {
    }

    static void setVolumePercent(SourceDataLine line, int volumePercent) {
        if (line == null) {
            return;
        }

        int boundedVolume = Math.clamp(volumePercent, 0, 100);
        if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            setControlValue(volumeControl, volumeControl.getMinimum()
                    + (volumeControl.getMaximum() - volumeControl.getMinimum()) * boundedVolume / 100.0f);
            return;
        }

        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = boundedVolume == 0
                    ? gainControl.getMinimum()
                    : (float) (20.0 * Math.log10(boundedVolume / 100.0));
            setControlValue(gainControl, Math.clamp(gain, gainControl.getMinimum(), gainControl.getMaximum()));
        }
    }

    private static void setControlValue(FloatControl control, float value) {
        control.setValue(Math.clamp(value, control.getMinimum(), control.getMaximum()));
    }
}
