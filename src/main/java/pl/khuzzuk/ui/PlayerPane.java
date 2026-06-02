package pl.khuzzuk.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlayerPane extends JPanel {
    private static final String PLAY_ICON = "▶";
    private static final String PAUSE_ICON = "⏸";
    private static final int PROGRESS_REFRESH_MILLIS = 500;
    JSlider progressSlider;
    JSlider volumeSlider;
    JButton previousButton;
    JButton playPauseButton;
    JButton stopButton;
    JButton nextButton;
    private final JLabel currentTimeLabel;
    private final JLabel durationTimeLabel;
    private final PlayerController playerController;
    private final Timer progressTimer;
    private final PlayerPaneModeler modeler = new PlayerPaneModeler();

    public PlayerPane(PlayerController playerController) {
        this.playerController = playerController;
        this.progressTimer = new Timer(PROGRESS_REFRESH_MILLIS, ignored -> updateProgress());
        setLayout(new BorderLayout(14, 0));
        modeler.modelPane(this);

        previousButton = createIconButton("⏮", "Previous track");
        previousButton.addActionListener(ignored -> playPrevious());
        playPauseButton = createIconButton(PLAY_ICON, "Play");
        playPauseButton.addActionListener(ignored -> playPause());
        stopButton = createIconButton("■", "Stop");
        stopButton.addActionListener(ignored -> stop());
        nextButton = createIconButton("⏭", "Next track");
        nextButton.addActionListener(ignored -> playNext());

        JPanel playControls = new JPanel();
        playControls.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        modeler.modelControlsPanel(playControls);
        playControls.add(previousButton);
        playControls.add(playPauseButton);
        playControls.add(stopButton);
        playControls.add(nextButton);
        JPanel playControlsWrapper = new JPanel(new GridBagLayout());
        modeler.modelControlsPanel(playControlsWrapper);
        playControlsWrapper.add(playControls);

        progressSlider = new JSlider(0, 100, 0);
        modeler.modelProgressSlider(progressSlider);
        ProgressMouseListener progressMouseListener = new ProgressMouseListener();
        progressSlider.addMouseListener(progressMouseListener);
        progressSlider.addMouseMotionListener(progressMouseListener);

        currentTimeLabel = createTimeLabel();
        durationTimeLabel = createTimeLabel();
        JPanel timeStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        modeler.modelStatusPanel(timeStatus);
        timeStatus.add(currentTimeLabel);
        JLabel timeSeparator = new JLabel("/");
        modeler.modelTimeSeparator(timeSeparator);
        timeStatus.add(timeSeparator);
        timeStatus.add(durationTimeLabel);
        updateTimeLabels(0, 0);

        volumeSlider = new JSlider(SwingConstants.VERTICAL, 0, 100, 60);
        modeler.modelVolumeSlider(volumeSlider);
        volumeSlider.setToolTipText("Glosnosc");
        volumeSlider.addChangeListener(this::volumeChange);
        playerController.setVolumePercent(volumeSlider.getValue());

        JPanel playStatus = new JPanel();
        playStatus.setLayout(new BorderLayout(14, 0));
        modeler.modelStatusPanel(playStatus);
        playStatus.add(playControlsWrapper, BorderLayout.WEST);
        playStatus.add(progressSlider, BorderLayout.CENTER);
        playStatus.add(timeStatus, BorderLayout.EAST);

        JPanel volumeStatus = new JPanel(new GridBagLayout());
        modeler.modelVolumePanel(volumeStatus);
        volumeStatus.add(volumeSlider);

        add(playStatus, BorderLayout.CENTER);
        add(volumeStatus, BorderLayout.WEST);
    }

    private void volumeChange(ChangeEvent e) {
        playerController.setVolumePercent(volumeSlider.getValue());
    }

    public void playPause() {
        boolean playing = playerController.playPause(this);
        updatePlaybackState(playing);
    }

    private void playNext() {
        updatePlaybackState(playerController.playNext());
    }

    private void playPrevious() {
        updatePlaybackState(playerController.playPrevious());
    }

    private void stop() {
        playerController.stop();
        progressTimer.stop();
        progressSlider.setValue(0);
        updateTimeLabels(0, 0);
        setPlayPauseButton(false);
    }

    private void updatePlaybackState(boolean playing) {
        setPlayPauseButton(playing);
        updateProgress();
        if (playing) {
            progressTimer.start();
        } else {
            progressTimer.stop();
        }
    }

    private void updateProgress() {
        boolean playing = playerController.isPlaying();
        int durationMillis = playerController.getCurrentDurationMillis();
        int positionMillis = playerController.getCurrentPositionMillis();
        progressSlider.setMaximum(Math.max(100, durationMillis));
        progressSlider.setValue(Math.clamp(positionMillis, 0, progressSlider.getMaximum()));
        updateTimeLabels(positionMillis, durationMillis);
        if (!playing) {
            progressTimer.stop();
            setPlayPauseButton(false);
        } else {
            setPlayPauseButton(true);
        }
    }

    private void seekProgress(MouseEvent event) {
        int durationMillis = playerController.getCurrentDurationMillis();
        if (durationMillis <= 0 || progressSlider.getWidth() <= 0) {
            return;
        }

        int positionMillis = Math.clamp(
                Math.round(event.getX() * durationMillis / (float) progressSlider.getWidth()),
                0,
                durationMillis);
        boolean playing = playerController.seekToMillis(positionMillis);
        progressSlider.setValue(positionMillis);
        updateTimeLabels(positionMillis, durationMillis);
        if (playing) {
            progressTimer.start();
        } else {
            progressTimer.stop();
        }
        setPlayPauseButton(playing);
    }

    private void setPlayPauseButton(boolean playing) {
        playPauseButton.setText(playing ? PAUSE_ICON : PLAY_ICON);
        playPauseButton.setToolTipText(playing ? "Pause" : "Play");
        playPauseButton.getAccessibleContext().setAccessibleName(playing ? "Pause" : "Play");
    }

    private JButton createIconButton(String symbol, String tooltip) {
        JButton button = new JButton(symbol);
        button.setToolTipText(tooltip);
        modeler.modelIconButton(button);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    private JLabel createTimeLabel() {
        JLabel label = new JLabel();
        modeler.modelTimeLabel(label);
        return label;
    }

    private void updateTimeLabels(int positionMillis, int durationMillis) {
        currentTimeLabel.setText(formatTime(positionMillis));
        durationTimeLabel.setText(formatTime(durationMillis));
    }

    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis) / 1000;
        int hours = totalSeconds / 3600;
        int minutes = totalSeconds % 3600 / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, seconds);
        }

        return "%d:%02d".formatted(minutes, seconds);
    }

    private class ProgressMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            seekProgress(event);
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            seekProgress(event);
        }
    }
}
