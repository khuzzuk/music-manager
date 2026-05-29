package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.Tag;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetadataEditDialog extends JDialog {
    private final Map<Tag, JComponent> editors = new EnumMap<>(Tag.class);
    private Optional<Map<Tag, Object>> result = Optional.empty();

    public static Optional<Map<Tag, Object>> showDialog(
            Component parent,
            SoundFileMetadata metadata,
            List<Tag> writableTags) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        MetadataEditDialog dialog = new MetadataEditDialog(owner, metadata, writableTags);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.result;
    }

    private MetadataEditDialog(Window owner, SoundFileMetadata metadata, List<Tag> writableTags) {
        super(owner, "Edycja metadanych", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 8));

        add(createForm(metadata, writableTags), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(520, 420));
        pack();
    }

    private JComponent createForm(SoundFileMetadata metadata, List<Tag> writableTags) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        for (Tag tag : writableTags) {
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            form.add(new JLabel(tag.label()), constraints);

            JComponent editor = createEditor(tag, tag.getValue(metadata));
            editors.put(tag, editor);

            constraints.gridx = 1;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            form.add(editor, constraints);
            constraints.gridy++;
        }

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JComponent createEditor(Tag tag, Object value) {
        if (tag == Tag.RATING) {
            return new RatingEditor(value instanceof Number number ? number.intValue() : 0);
        }

        JTextField textField = new JTextField(value == null ? "" : value.toString(), 34);
        return textField;
    }

    private JComponent createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Anuluj");
        JButton saveButton = new JButton("Zapisz");

        cancelButton.addActionListener(event -> dispose());
        saveButton.addActionListener(event -> {
            result = Optional.of(readValues());
            dispose();
        });
        getRootPane().setDefaultButton(saveButton);

        buttons.add(cancelButton);
        buttons.add(saveButton);
        return buttons;
    }

    private Map<Tag, Object> readValues() {
        Map<Tag, Object> values = new EnumMap<>(Tag.class);
        for (Map.Entry<Tag, JComponent> entry : editors.entrySet()) {
            JComponent editor = entry.getValue();
            if (editor instanceof RatingEditor ratingEditor) {
                values.put(entry.getKey(), ratingEditor.getRating());
            } else if (editor instanceof JTextField textField) {
                String text = textField.getText().trim();
                values.put(entry.getKey(), text.isEmpty() ? null : text);
            }
        }

        return values;
    }
}
