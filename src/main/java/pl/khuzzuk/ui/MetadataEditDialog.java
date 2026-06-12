package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.SoundFileMetadata;
import pl.khuzzuk.metadata.Tag;

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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MetadataEditDialog extends JDialog {
    private final MetadataIndexReaderService metadataIndexReaderService;
    private final Map<Tag, JComponent> editors = new EnumMap<>(Tag.class);
    private final Map<Tag, Object> initialValues = new EnumMap<>(Tag.class);
    private final Map<Tag, Color> editorBackgrounds = new EnumMap<>(Tag.class);
    private final MetadataEditDialogModeler modeler = new MetadataEditDialogModeler();
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

        modeler.modelDialog(this);
        pack();
    }

    private JComponent createForm(List<SoundFileMetadata> metadataItems, List<Tag> writableTags) {
        JPanel form = new JPanel(new GridBagLayout());
        modeler.modelForm(form);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = modeler.formFieldInsets();
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;

        JLabel pathLabel = new JLabel(createPathLabelText(metadataItems));
        modeler.modelPathLabel(pathLabel);
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        form.add(pathLabel, constraints);
        constraints.gridy++;
        constraints.gridwidth = 1;

        for (Tag tag : writableTags) {
            FieldState fieldState = getFieldState(metadataItems, tag);
            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(tag.label());
            modeler.modelFieldLabel(label);
            form.add(label, constraints);

            JComponent editor = createEditor(tag, fieldState.initialValue());
            modeler.modelEditor(editor);
            editor.setEnabled(fieldState.editable());
            modeler.modelFieldState(label, editor, fieldState.editable());
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
        modeler.modelFormScrollPane(scrollPane);
        return scrollPane;
    }

    private String createPathLabelText(List<SoundFileMetadata> metadataItems) {
        if (metadataItems == null || metadataItems.isEmpty()) {
            return "Sciezka: ";
        }

        StringBuilder text = new StringBuilder("<html><body style='width: 460px'>");
        text.append(metadataItems.size() == 1 ? "Sciezka: " : "Sciezki:");
        for (SoundFileMetadata metadata : metadataItems) {
            if (metadata.path() == null) {
                continue;
            }

            if (metadataItems.size() > 1) {
                text.append("<br>");
            }
            text.append(escapeHtml(metadata.path().toAbsolutePath().normalize().toString()));
        }
        text.append("</body></html>");
        return text.toString();
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private JComponent createEditor(Tag tag, Object value) {
        if (tag == Tag.RATING) {
            return new RatingEditor(value instanceof Number number ? number.intValue() : 0);
        }

        String text = value == null ? "" : value.toString();
        if (MetadataSuggestionTags.supports(tag)) {
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

        modeler.modelEditorChangeState(editor, isChanged(tag), editorBackgrounds.get(tag));
    }

    private JComponent createButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        modeler.modelButtonsPanel(buttons);
        JButton cancelButton = new JButton("Anuluj");
        JButton saveButton = new JButton("Zapisz");
        modeler.modelCancelButton(cancelButton);
        modeler.modelSaveButton(saveButton);

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
