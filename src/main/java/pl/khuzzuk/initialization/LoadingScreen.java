package pl.khuzzuk.initialization;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import java.awt.Font;

public class LoadingScreen extends JWindow {
    public LoadingScreen() {
        JLabel label = new JLabel("Wczytywanie...", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        add(label);

        setSize(400, 200);
        setLocationRelativeTo(null);
    }
}
