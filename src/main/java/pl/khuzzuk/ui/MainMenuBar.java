package pl.khuzzuk.ui;

import pl.khuzzuk.Context;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Window;

public class MainMenuBar extends JMenuBar {
    private final Context context;
    private IndexDirectoriesDialog indexDirectoriesDialog;

    public MainMenuBar(Context context) {
        this.context = context;
        JMenu menu = new JMenu("Plik");
        JMenuItem menuIndexItem = new JMenuItem("Indeks");
        menuIndexItem.addActionListener(ignored -> chooseIndexDirectory());
        menu.add(menuIndexItem);
        add(menu);
    }

    private void chooseIndexDirectory() {
        if (indexDirectoriesDialog == null || !indexDirectoriesDialog.isDisplayable()) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            indexDirectoriesDialog = new IndexDirectoriesDialog(owner, context);
            indexDirectoriesDialog.setLocationRelativeTo(this);
        }

        indexDirectoriesDialog.showDialog();
    }
}
