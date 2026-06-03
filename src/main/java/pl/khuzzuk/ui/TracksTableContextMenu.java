package pl.khuzzuk.ui;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class TracksTableContextMenu extends JPopupMenu {
    private final TracksTableModeler modeler;

    TracksTableContextMenu(
            TracksTableModeler modeler,
            Runnable addToPlaylistAction,
            Runnable titleFromFileNameAction,
            Runnable deleteAction,
            Runnable editMetadataAction) {
        this.modeler = modeler;
        modeler.modelContextMenu(this);
        add(contextMenuItem("Dodaj do playlisty", addToPlaylistAction));
        add(contextMenuItem("Ustaw tytul z nazwy pliku", titleFromFileNameAction));
        add(contextMenuItem("Usun", deleteAction));
        add(contextMenuItem("Edytuj metadane", editMetadataAction));
    }

    private JMenuItem contextMenuItem(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        modeler.modelContextMenuItem(item);
        item.addActionListener(ignored -> action.run());
        return item;
    }
}
