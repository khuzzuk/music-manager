package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

    public PlayerPane(PlayerController playerController) {
        this.playerController = playerController;
        this.progressTimer = new Timer(PROGRESS_REFRESH_MILLIS, ignored -> updateProgress());
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        previousButton = createIconButton("⏮", "Previous track");
        previousButton.addActionListener(ignored -> playPrevious());
        playPauseButton = createIconButton(PLAY_ICON, "Play");
        playPauseButton.addActionListener(ignored -> playPause());
        stopButton = createIconButton("■", "Stop");
        stopButton.addActionListener(ignored -> stop());
        nextButton = createIconButton("⏭", "Next track");
        nextButton.addActionListener(ignored -> playNext());

        JPanel playControls = new JPanel();
        playControls.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        playControls.add(previousButton);
        playControls.add(playPauseButton);
        playControls.add(stopButton);
        playControls.add(nextButton);
        JPanel playControlsWrapper = new JPanel(new GridBagLayout());
        playControlsWrapper.add(playControls);

        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setPaintTicks(false);
        progressSlider.setPaintLabels(false);
        progressSlider.setFocusable(false);
        progressSlider.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));
        progressSlider.addMouseListener(new ProgressMouseListener());

        currentTimeLabel = createTimeLabel();
        durationTimeLabel = createTimeLabel();
        JPanel timeStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 5));
        timeStatus.add(currentTimeLabel);
        timeStatus.add(new JLabel("/"));
        timeStatus.add(durationTimeLabel);
        updateTimeLabels(0, 0);

        volumeSlider = new JSlider(SwingConstants.VERTICAL, 0, 100, 60);
        volumeSlider.setFocusable(false);
        volumeSlider.setPaintTicks(false);
        volumeSlider.setPaintLabels(false);
        volumeSlider.setPreferredSize(new Dimension(20, 100));
        volumeSlider.setToolTipText("Glosnosc");
        volumeSlider.addChangeListener(this::volumeChange);

        JPanel playStatus = new JPanel();
        playStatus.setLayout(new BorderLayout(5, 5));
        playStatus.add(playControlsWrapper, BorderLayout.WEST);
        playStatus.add(progressSlider, BorderLayout.CENTER);
        playStatus.add(timeStatus, BorderLayout.EAST);

        add(playStatus, BorderLayout.CENTER);
        add(volumeSlider, BorderLayout.WEST);
    }

    private void volumeChange(ChangeEvent e) {
        if (!volumeSlider.getValueIsAdjusting()) {
            System.out.println(volumeSlider.getValue());
        }
    }

    private void playPause() {
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
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setPreferredSize(new Dimension(48, 36));
        button.setMinimumSize(new Dimension(48, 36));
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    private JLabel createTimeLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(48, 20));
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
    }
}
