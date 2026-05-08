package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class PlayerPane extends JPanel {
    JSlider progressSlider;
    JSlider volumeSlider;
    JButton previousButton;
    JButton playPauseButton;
    JButton stopButton;
    JButton nextButton;

    public PlayerPane() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        previousButton = createIconButton("⏮", "Previous track");
        playPauseButton = createIconButton("▶", "Play");
        stopButton = createIconButton("■", "Stop");
        nextButton = createIconButton("⏭", "Next track");
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
        progressSlider.setEnabled(false);

        volumeSlider = new JSlider(SwingConstants.VERTICAL, 0, 100, 60);
        volumeSlider.setFocusable(false);
        volumeSlider.setPaintTicks(false);
        volumeSlider.setPaintLabels(false);
        volumeSlider.setPreferredSize(new Dimension(20, 100));
        volumeSlider.setToolTipText("Głośność");
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
}
