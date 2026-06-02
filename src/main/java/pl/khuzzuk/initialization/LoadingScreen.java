package pl.khuzzuk.initialization;

import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;

public class LoadingScreen extends JWindow {
    public LoadingScreen() {
        JLabel label = new JLabel("Wczytywanie...", SwingConstants.CENTER);
        LoadingScreenModeler modeler = new LoadingScreenModeler();
        modeler.modelLoadingLabel(label);
        add(label);

        setSize(400, 200);
        setLocationRelativeTo(null);
    }
}
