package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.IndexReaderService;
import pl.khuzzuk.index.IndexService;
import pl.khuzzuk.player.PlaylistSoundFile;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class ContentPane extends JPanel {
    private static final String[] TRACK_COLUMNS = {"Nazwa", "Sciezka"};
    private final FileTree fileTree;
    private final JTable tracksTable;
    private final JList<PlaylistSoundFile> playlist;

    public ContentPane(IndexReaderService indexReaderService, IndexService indexService) {
        super(new GridBagLayout());

        this.tracksTable = new JTable();
        this.playlist = new JList<>();
        this.fileTree = new FileTree(indexReaderService, indexService, this::showMappedFiles);
        showMappedFiles(List.of());

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

    private void showMappedFiles(List<IndexItem> files) {
        DefaultTableModel model = new DefaultTableModel(TRACK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        files.forEach(file -> model.addRow(new Object[]{file.getName(), file.getPath()}));
        tracksTable.setModel(model);
    }
}
