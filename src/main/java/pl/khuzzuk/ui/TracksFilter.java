package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.logging.ErrorReporter;
import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.Tag;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

class TracksFilter extends JPanel {
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final Consumer<Selection> selectionConsumer;
    private final JComboBox<Tag> fieldComboBox = new JComboBox<>();
    private final DefaultListModel<String> valuesModel = new DefaultListModel<>();
    private final JList<String> valuesList = new ToggleSelectionList(valuesModel);
    private SwingWorker<List<String>, Void> valuesWorker;
    private boolean loadingValues;

    TracksFilter(Context context, Consumer<Selection> selectionConsumer) {
        super(new GridBagLayout());
        this.metadataIndexReaderService = context.metadataIndexReaderService();
        this.selectionConsumer = selectionConsumer;

        TracksFilterModeler modeler = new TracksFilterModeler();
        modeler.modelPanel(this);
        configureFieldComboBox(context.settingsService().getSettings().lastTracksFilterTag(), modeler);
        configureValuesList(modeler);
        layoutComponents(modeler);

        fieldComboBox.addActionListener(ignored -> changeField());
        valuesList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !loadingValues) {
                notifySelection();
            }
        });
        context.indexService().addIndexListener(ignored -> SwingUtilities.invokeLater(this::loadValues));
        loadValues();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        UiTheme.paintRoundedBackground(this, graphics, getBackground());
        super.paintComponent(graphics);
    }

    Tag getSelectedTag() {
        Tag tag = (Tag) fieldComboBox.getSelectedItem();
        return tag == null ? Tag.MOOD : tag;
    }

    private void configureFieldComboBox(Tag selectedTag, TracksFilterModeler modeler) {
        fieldComboBox.setModel(new DefaultComboBoxModel<>(Tag.values()));
        fieldComboBox.setSelectedItem(selectedTag == null ? Tag.MOOD : selectedTag);
        modeler.modelFieldComboBox(fieldComboBox);
    }

    private void configureValuesList(TracksFilterModeler modeler) {
        valuesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        modeler.modelValuesList(valuesList);
    }

    private void layoutComponents(TracksFilterModeler modeler) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = modeler.fieldInsets();
        add(fieldComboBox, c);

        JScrollPane valuesScrollPane = new JScrollPane(valuesList);
        modeler.modelScrollPane(valuesScrollPane);
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.insets = new java.awt.Insets(0, 0, 0, 0);
        add(valuesScrollPane, c);
    }

    private void changeField() {
        loadingValues = true;
        try {
            valuesList.clearSelection();
        } finally {
            loadingValues = false;
        }
        notifySelection();
        loadValues();
    }

    private void loadValues() {
        if (valuesWorker != null && !valuesWorker.isDone()) {
            valuesWorker.cancel(true);
        }

        Tag tag = getSelectedTag();
        valuesWorker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws IOException {
                return metadataIndexReaderService.readValues(tag);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    replaceValues(get());
                } catch (Exception e) {
                    ErrorReporter.log("Cannot read tracks filter values.", e);
                    replaceValues(List.of());
                }
            }
        };
        valuesWorker.execute();
    }

    private void replaceValues(List<String> values) {
        loadingValues = true;
        try {
            valuesModel.clear();
            values.forEach(valuesModel::addElement);
            valuesList.clearSelection();
        } finally {
            loadingValues = false;
        }
    }

    private void notifySelection() {
        selectionConsumer.accept(new Selection(getSelectedTag(), valuesList.getSelectedValuesList()));
    }

    record Selection(Tag tag, List<String> values) {
        Selection {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    private static class ToggleSelectionList extends JList<String> {
        private ToggleSelectionList(DefaultListModel<String> model) {
            super(model);
        }

        @Override
        protected void processMouseEvent(MouseEvent event) {
            if (shouldToggleSelection(event)) {
                int index = locationToIndex(event.getPoint());
                removeSelectionInterval(index, index);
                requestFocusInWindow();
                event.consume();
                return;
            }
            super.processMouseEvent(event);
        }

        private boolean shouldToggleSelection(MouseEvent event) {
            if (event.getID() != MouseEvent.MOUSE_PRESSED || !SwingUtilities.isLeftMouseButton(event)
                    || event.isShiftDown() || event.isControlDown() || event.isMetaDown()) {
                return false;
            }

            int index = locationToIndex(event.getPoint());
            if (index < 0 || !isSelectedIndex(index)) {
                return false;
            }

            Rectangle bounds = getCellBounds(index, index);
            return bounds != null && bounds.contains(event.getPoint());
        }
    }
}
