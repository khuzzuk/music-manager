package pl.khuzzuk.ui;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;

class MainWindowModeler {
    private static final String WINDOW_ICON_PATH = "/icons/music-manager-icon.png";
    private static final List<Integer> WINDOW_ICON_SIZES = List.of(16, 24, 32, 48, 64, 128, 256);

    void modelWindow(JFrame window) {
        UiTheme.installDefaults();
        window.getContentPane().setBackground(UiTheme.BACKGROUND);
        modelWindowIcon(window);
    }

    void modelPlayerPaneContainer(JPanel container) {
        container.setOpaque(false);
        container.setBackground(UiTheme.BACKGROUND);
        container.setBorder(UiTheme.empty(0, 10, 10, 10));
    }

    private void modelWindowIcon(JFrame window) {
        try {
            URL iconUrl = getClass().getResource(WINDOW_ICON_PATH);
            if (iconUrl == null) {
                return;
            }

            BufferedImage icon = ImageIO.read(iconUrl);
            List<Image> icons = WINDOW_ICON_SIZES.stream()
                    .map(size -> icon.getScaledInstance(size, size, Image.SCALE_SMOOTH))
                    .toList();
            window.setIconImages(icons);
            setTaskbarIcon(icon);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load application icon.", e);
        } catch (UnsupportedOperationException | SecurityException e) {
            // Window icons are non-critical; the application can run without them.
        }
    }

    private void setTaskbarIcon(Image icon) {
        if (!Taskbar.isTaskbarSupported()) {
            return;
        }

        Taskbar taskbar = Taskbar.getTaskbar();
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            taskbar.setIconImage(icon);
        }
    }
}
