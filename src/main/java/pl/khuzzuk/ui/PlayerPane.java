package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

    private class ProgressMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent event) {
            seekProgress(event);
        }
    }
}
