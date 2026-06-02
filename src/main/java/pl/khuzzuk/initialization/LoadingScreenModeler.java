package pl.khuzzuk.initialization;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;

class LoadingScreenModeler {
    private static final Color BACKGROUND = new Color(38, 32, 29);
    private static final Color TEXT = new Color(250, 246, 238);
    private static final Color ACCENT = new Color(155, 109, 43);

    void modelLoadingLabel(JLabel label) {
        label.setFont(new Font("Georgia", Font.BOLD, 24));
        label.setForeground(TEXT);
        label.setBackground(BACKGROUND);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createLineBorder(ACCENT));
    }
}
