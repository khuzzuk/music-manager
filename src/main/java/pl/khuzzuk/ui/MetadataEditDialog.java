package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.MetadataIndexReaderService;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.Objects;
import java.util.Optional;

public class MetadataEditDialog extends JDialog {
    private static final Color CHANGED_FIELD_COLOR = new Color(224, 240, 255);
    private static final List<Tag> SUGGESTED_TAGS = List.of(
            Tag.ARTIST,
            Tag.COMPOSER,
            Tag.CONDUCTOR,
            Tag.GENRE,
            Tag.MOOD,
            Tag.TEMPO,
            Tag.OCCASION);
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final Map<Tag, JComponent> editors = new EnumMap<>(Tag.class);
    private final Map<Tag, Object> initialValues = new EnumMap<>(Tag.class);
    private final Map<Tag, Color> editorBackgrounds = new EnumMap<>(Tag.class);
    private Map<Tag, Object> result;

    public static Optional<Map<Tag, Object>> showDialog(
            Component parent,
            List<SoundFileMetadata> metadataItems,
            List<Tag> writableTags,
            MetadataIndexReaderService metadataIndexReaderService) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        MetadataEditDialog dialog = new MetadataEditDialog(
                owner,
                metadataItems,
                writableTags,
                metadataIndexReaderService);
        dialog.setLocationNearTopLeft(owner);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.result);
    }

    private MetadataEditDialog(
            Window owner,
            List<SoundFileMetadata> metadataItems,
            List<Tag> writableTags,
            MetadataIndexReaderService metadataIndexReaderService) {
        super(owner, "Edycja metadanych", ModalityType.APPLICATION_MODAL);
        this.metadataIndexReaderService = metadataIndexReaderService;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 8));

        add(createForm(metadataItems, writableTags), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(520, 420));
        pack();
    }

    private JComponent createForm(List<SoundFileMetadata> metadataItems, List<Tag> writableTags) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        for (Tag tag : writableTags) {
            FieldState fieldState = getFieldState(metadataItems, tag);
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            form.add(new JLabel(tag.label()), constraints);

            JComponent editor = createEditor(tag, fieldState.initialValue());
            editor.setEnabled(fieldState.editable());
            editors.put(tag, editor);
            initialValues.put(tag, fieldState.initialValue());
            editorBackgrounds.put(tag, editor.getBackground());
            installChangeListener(tag, editor);

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

        String text = value == null ? "" : value.toString();
        if (SUGGESTED_TAGS.contains(tag)) {
            return new MetadataSuggestionTextField(text, 34, tag, metadataIndexReaderService);
        }

        return new JTextField(text, 34);
    }

    private void installChangeListener(Tag tag, JComponent editor) {
        if (editor instanceof RatingEditor ratingEditor) {
            ratingEditor.addChangeListener(ignored -> updateChangeLabel(tag));
        } else if (editor instanceof JTextField textField) {
            textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    updateChangeLabel(tag);
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    updateChangeLabel(tag);
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    updateChangeLabel(tag);
                }
            });
        }
    }

    private void updateChangeLabel(Tag tag) {
        JComponent editor = editors.get(tag);
        if (editor == null || !editor.isEnabled()) {
            return;
        }

        editor.setBackground(isChanged(tag) ? CHANGED_FIELD_COLOR : editorBackgrounds.get(tag));
    }

    private JComponent createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Anuluj");
        JButton saveButton = new JButton("Zapisz");

        cancelButton.addActionListener(ignored -> dispose());
        saveButton.addActionListener(ignored -> {
            result = readValues();
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
            if (!entry.getValue().isEnabled() || !isChanged(entry.getKey())) {
                continue;
            }

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

    private boolean isChanged(Tag tag) {
        return !Objects.equals(normalizeValue(tag, initialValues.get(tag)), normalizeValue(tag, readValue(tag)));
    }

    private Object readValue(Tag tag) {
        JComponent editor = editors.get(tag);
        if (editor instanceof RatingEditor ratingEditor) {
            return ratingEditor.getRating();
        }
        if (editor instanceof JTextField textField) {
            String text = textField.getText().trim();
            return text.isEmpty() ? null : text;
        }

        return null;
    }

    private FieldState getFieldState(List<SoundFileMetadata> metadataItems, Tag tag) {
        Object commonValue = null;
        boolean firstValue = true;
        for (SoundFileMetadata metadata : metadataItems) {
            Object value = normalizeValue(tag, tag.getValue(metadata));
            if (firstValue) {
                commonValue = value;
                firstValue = false;
            } else if (!Objects.equals(commonValue, value)) {
                return new FieldState(null, false);
            }
        }

        return new FieldState(commonValue == null ? emptyValue(tag) : commonValue, true);
    }

    private Object normalizeValue(Tag tag, Object value) {
        if (tag == Tag.RATING) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value == null || value.toString().isBlank()) {
                return 0;
            }
            return Integer.parseInt(value.toString().trim());
        }

        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Object emptyValue(Tag tag) {
        return tag == Tag.RATING ? 0 : null;
    }

    private void setLocationNearTopLeft(Window owner) {
        if (owner == null) {
            setLocation(80, 80);
            return;
        }

        setLocation(owner.getX() + 48, owner.getY() + 64);
    }

    private record FieldState(Object initialValue, boolean editable) {
    }
}
