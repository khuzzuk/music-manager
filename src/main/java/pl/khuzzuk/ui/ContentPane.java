package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.metadata.MetadataReaderService;
import pl.khuzzuk.player.PlaylistSoundFile;
import pl.khuzzuk.settings.SettingsService;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ContentPane extends JPanel {

    public ContentPane(
            SettingsService settingsService,
            IndexReaderService indexReaderService,
            IndexService indexService,
            MetadataReaderService metadataReaderService) {
        super(new GridBagLayout());

        TracksTable tracksTable = new TracksTable(settingsService, metadataReaderService);
        JList<PlaylistSoundFile> playlist = new JList<>();
        FileTree fileTree = new FileTree(indexReaderService, indexService, tracksTable::showMappedFiles);

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
        add(new JScrollPane(tracksTable), c);
        c.gridx = 2;
        c.weightx = 0.2;
        add(playlist, c);
    }
}
