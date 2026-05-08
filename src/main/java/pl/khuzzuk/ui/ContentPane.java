package pl.khuzzuk.ui;

import pl.khuzzuk.player.PlaylistSoundFile;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ContentPane extends JPanel {
    private JTree fileTree;
    private JTable tracksTable;
    private JList<PlaylistSoundFile> playlist;

    public ContentPane() {
        super(new GridBagLayout());

        this.tracksTable = new JTable();
        this.playlist = new JList<>();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Muzyka");
        root.add(new DefaultMutableTreeNode("1"));
        root.add(new DefaultMutableTreeNode("2"));
        fileTree = new JTree(root);

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
