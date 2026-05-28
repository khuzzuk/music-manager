package pl.khuzzuk.ui;

import pl.khuzzuk.index.IndexItem;
import pl.khuzzuk.index.SoundFileIndexItem;
import pl.khuzzuk.metadata.SoundFileMetadata;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class TracksTable extends JTable {
    private static final String[] TRACK_COLUMNS = {
            "Tytul",
            "Album",
            "Kompozytor",
            "Rating",
            "Mood",
            "Movement",
            "Occasion"
    };

    public TracksTable() {
        showMappedFiles(List.of());
    }

    public void showMappedFiles(List<IndexItem> files) {
        DefaultTableModel model = new DefaultTableModel(TRACK_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        files.forEach(file -> {
            SoundFileMetadata metadata = getMetadata(file);
            model.addRow(new Object[]{
                    metadata.title(),
                    metadata.album(),
                    metadata.composer(),
                    metadata.rating(),
                    metadata.mood(),
                    metadata.movement(),
                    metadata.occasion()
            });
        });
        setModel(model);
    }

    private SoundFileMetadata getMetadata(IndexItem file) {
        if (file instanceof SoundFileIndexItem soundFileIndexItem) {
            return soundFileIndexItem.getMetadata();
        }

        return SoundFileMetadata.empty(file.getPath());
    }
}
