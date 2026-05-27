package pl.khuzzuk.ui;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.io.File;

public class MainMenuBar extends JMenuBar {
    void init() {
        JMenu menu = new JMenu("Plik");
        JMenuItem menuIndexItem = new JMenuItem("Indeks");
        menuIndexItem.addActionListener(event -> chooseIndexDirectory());
        menu.add(menuIndexItem);
        add(menu);
    }

    private void chooseIndexDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Wybierz katalog do indeksowania");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            System.out.println("Wybrany katalog: " + selectedDirectory.getAbsolutePath());
        }
    }
}
