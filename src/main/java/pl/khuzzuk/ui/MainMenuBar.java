package pl.khuzzuk.ui;

import pl.khuzzuk.settings.SettingsService;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Window;

public class MainMenuBar extends JMenuBar {
    private final SettingsService settingsService;
    private IndexDirectoriesDialog indexDirectoriesDialog;

    public MainMenuBar(SettingsService settingsService) {
        this.settingsService = settingsService;
        JMenu menu = new JMenu("Plik");
        JMenuItem menuIndexItem = new JMenuItem("Indeks");
        menuIndexItem.addActionListener(event -> chooseIndexDirectory());
        menu.add(menuIndexItem);
        add(menu);
    }

    private void chooseIndexDirectory() {
        if (indexDirectoriesDialog == null || !indexDirectoriesDialog.isDisplayable()) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            indexDirectoriesDialog = new IndexDirectoriesDialog(owner, settingsService);
            indexDirectoriesDialog.setLocationRelativeTo(this);
        }

        indexDirectoriesDialog.showDialog();
    }
}
