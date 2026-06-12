package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.Tag;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.EventObject;

class TracksTableCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final TracksTableModeler modeler;
    private JTextField editorField;

    TracksTableCellEditor(MetadataIndexReaderService metadataIndexReaderService, TracksTableModeler modeler) {
        this.metadataIndexReaderService = metadataIndexReaderService;
        this.modeler = modeler;
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean selected,
            int row,
            int column) {
        String text = value == null ? "" : value.toString();
        editorField = createEditorField(table, text, column);
        modeler.modelCellEditorField(editorField);
        editorField.selectAll();
        return editorField;
    }

    @Override
    public Object getCellEditorValue() {
        return editorField == null ? null : editorField.getText();
    }

    @Override
    public boolean stopCellEditing() {
        closeSuggestions();
        return super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        closeSuggestions();
        super.cancelCellEditing();
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        if (event instanceof MouseEvent mouseEvent) {
            return mouseEvent.getClickCount() >= 2;
        }

        return true;
    }

    private JTextField createEditorField(JTable table, String text, int viewColumn) {
        Tag tag = tagForColumn(table, viewColumn);
        if (MetadataSuggestionTags.supports(tag)) {
            return new SuggestionCellEditorField(
                    text,
                    0,
                    tag,
                    metadataIndexReaderService,
                    this::stopCellEditing,
                    this::cancelCellEditing);
        }

        return new CellEditorField(text);
    }

    private Tag tagForColumn(JTable table, int viewColumn) {
        if (table == null || viewColumn < 0) {
            return null;
        }

        Object identifier = table.getColumnModel().getColumn(viewColumn).getIdentifier();
        return identifier instanceof Tag tag ? tag : null;
    }

    private void closeSuggestions() {
        if (editorField instanceof MetadataSuggestionTextField suggestionTextField) {
            suggestionTextField.closeSuggestions();
        }
    }

    private class CellEditorField extends JTextField {
        private CellEditorField(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            modeler.paintCellEditorField(this, graphics);
            super.paintComponent(graphics);
        }
    }

    private class SuggestionCellEditorField extends MetadataSuggestionTextField {
        private SuggestionCellEditorField(
                String text,
                int columns,
                Tag tag,
                MetadataIndexReaderService metadataIndexReaderService,
                Runnable enterFallback,
                Runnable escapeFallback) {
            super(text, columns, tag, metadataIndexReaderService, enterFallback, escapeFallback);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            modeler.paintCellEditorField(this, graphics);
            super.paintComponent(graphics);
        }
    }
}
