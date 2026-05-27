package pl.khuzzuk.ui;

import pl.khuzzuk.player.PlaylistSoundFile;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ContentPane extends JPanel {
    private final FileTree fileTree;
    private final JTable tracksTable;
    private final JList<PlaylistSoundFile> playlist;

    public ContentPane() {
        super(new GridBagLayout());

        this.tracksTable = new JTable();
        this.playlist = new JList<>();
        this.fileTree = new FileTree();

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1.0;
        c.insets = new Insets(0, 5, 0, 5);

        c.gridx = 0;
        c.weightx = 0.2;
        add(fileTree, c);
        c.gridx = 1;
        c.weightx = 0.6;
        add(tracksTable, c);
        c.gridx = 2;
        c.weightx = 0.2;
        add(playlist, c);
    }
}
