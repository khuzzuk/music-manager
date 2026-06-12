package pl.khuzzuk.ui;

import pl.khuzzuk.metadata.MetadataIndexReaderService;
import pl.khuzzuk.metadata.Tag;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

class MetadataSuggestionTextField extends JTextField {
    private static final int MAX_SUGGESTIONS = 10;
    private static final int VISIBLE_ROWS = 8;

    private final MetadataIndexReaderService metadataIndexReaderService;
    private final Tag tag;
    private final DefaultListModel<String> suggestionsModel = new DefaultListModel<>();
    private final JList<String> suggestionsList = new JList<>(suggestionsModel);
    private final JPopupMenu suggestionsPopup = new JPopupMenu();
    private final JScrollPane suggestionsScrollPane = new JScrollPane(suggestionsList);
    private final MetadataSuggestionTextFieldModeler modeler = new MetadataSuggestionTextFieldModeler();
    private final Runnable enterFallback;
    private final Runnable escapeFallback;
    private boolean applyingSuggestion;
    private SwingWorker<List<String>, Void> suggestionsWorker;
    private int suggestionsRequest;

    MetadataSuggestionTextField(
            String text,
            int columns,
            Tag tag,
            MetadataIndexReaderService metadataIndexReaderService) {
        this(text, columns, tag, metadataIndexReaderService, null, null);
    }

    MetadataSuggestionTextField(
            String text,
            int columns,
            Tag tag,
            MetadataIndexReaderService metadataIndexReaderService,
            Runnable enterFallback,
            Runnable escapeFallback) {
        super(text, columns);
        this.tag = tag;
        this.metadataIndexReaderService = metadataIndexReaderService;
        this.enterFallback = enterFallback;
        this.escapeFallback = escapeFallback;
        configureSuggestionsList();
        installSuggestionListeners();
        installSuggestionKeyBindings();
    }

    private void configureSuggestionsList() {
        suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionsList.setVisibleRowCount(VISIBLE_ROWS);
        suggestionsList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 1) {
                    applySelectedSuggestion();
                }
            }
        });

        modeler.modelSuggestionsPopup(suggestionsPopup);
        modeler.modelSuggestionsScrollPane(suggestionsScrollPane);
        suggestionsPopup.add(suggestionsScrollPane);
    }

    private void installSuggestionListeners() {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                scheduleSuggestionsUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                scheduleSuggestionsUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                scheduleSuggestionsUpdate();
            }
        });
    }

    private void installSuggestionKeyBindings() {
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "suggestionDown");
        getActionMap().put("suggestionDown", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                moveSelection(1);
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "suggestionUp");
        getActionMap().put("suggestionUp", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                moveSelection(-1);
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "applySuggestion");
        getActionMap().put("applySuggestion", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (suggestionsPopup.isVisible()) {
                    applySelectedSuggestion();
                } else {
                    activateEnterFallback();
                }
            }
        });

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "hideSuggestions");
        getActionMap().put("hideSuggestions", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (suggestionsPopup.isVisible()) {
                    hideSuggestions();
                    activateEscapeFallback();
                } else {
                    activateEscapeFallback();
                }
            }
        });
    }

    @Override
    protected boolean processKeyBinding(
            KeyStroke keyStroke,
            KeyEvent event,
            int condition,
            boolean pressed) {
        if (pressed && handleSuggestionKey(event)) {
            return true;
        }

        return super.processKeyBinding(keyStroke, event, condition, pressed);
    }

    private boolean handleSuggestionKey(KeyEvent event) {
        if (event.getModifiersEx() != 0) {
            return false;
        }

        if (event.getKeyCode() == KeyEvent.VK_DOWN) {
            moveSelection(1);
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_UP) {
            moveSelection(-1);
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            if (suggestionsPopup.isVisible()) {
                applySelectedSuggestion();
            } else {
                activateEnterFallback();
            }
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (suggestionsPopup.isVisible()) {
                hideSuggestions();
            }
            activateEscapeFallback();
            return true;
        }

        return false;
    }

    private void scheduleSuggestionsUpdate() {
        if (applyingSuggestion) {
            return;
        }

        SwingUtilities.invokeLater(this::updateSuggestions);
    }

    private void updateSuggestions() {
        if (!isShowing() || !isEnabled()) {
            cancelSuggestionsWorker();
            hideSuggestions();
            return;
        }

        String prefix = getText().trim();
        if (prefix.isEmpty()) {
            cancelSuggestionsWorker();
            hideSuggestions();
            return;
        }

        int request = ++suggestionsRequest;
        cancelSuggestionsWorker();
        suggestionsWorker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws IOException {
                return metadataIndexReaderService.suggestValues(tag, prefix, MAX_SUGGESTIONS);
            }

            @Override
            protected void done() {
                if (isCancelled() || request != suggestionsRequest || !prefix.equals(getText().trim())) {
                    return;
                }

                try {
                    showSuggestions(get());
                } catch (Exception e) {
                    hideSuggestions();
                }
            }
        };
        suggestionsWorker.execute();
    }

    private void showSuggestions(List<String> suggestions) {
        suggestionsModel.clear();
        for (String suggestion : suggestions) {
            if (!suggestion.equals(getText().trim())) {
                suggestionsModel.addElement(suggestion);
            }
        }

        if (suggestionsModel.isEmpty()) {
            hideSuggestions();
            return;
        }

        suggestionsList.setSelectedIndex(0);
        modeler.modelSuggestionsSize(suggestionsList, suggestionsScrollPane, suggestionsPopup, getWidth(), VISIBLE_ROWS);
        suggestionsPopup.show(this, 0, getHeight());
    }

    private void moveSelection(int step) {
        if (!suggestionsPopup.isVisible()) {
            updateSuggestions();
            return;
        }

        int size = suggestionsModel.getSize();
        if (size == 0) {
            hideSuggestions();
            return;
        }

        int index = suggestionsList.getSelectedIndex();
        int nextIndex = Math.clamp(index + step, 0, size - 1);
        suggestionsList.setSelectedIndex(nextIndex);
        suggestionsList.ensureIndexIsVisible(nextIndex);
    }

    private void applySelectedSuggestion() {
        String suggestion = suggestionsList.getSelectedValue();
        if (suggestion == null) {
            hideSuggestions();
            return;
        }

        applyingSuggestion = true;
        try {
            setText(suggestion);
            setCaretPosition(getText().length());
        } finally {
            applyingSuggestion = false;
        }
        hideSuggestions();
    }

    private void hideSuggestions() {
        suggestionsPopup.setVisible(false);
    }

    void closeSuggestions() {
        cancelSuggestionsWorker();
        hideSuggestions();
    }

    private void activateEnterFallback() {
        if (enterFallback != null) {
            enterFallback.run();
            return;
        }

        javax.swing.JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null && rootPane.getDefaultButton() != null) {
            rootPane.getDefaultButton().doClick();
        }
    }

    private void activateEscapeFallback() {
        if (escapeFallback != null) {
            escapeFallback.run();
        }
    }

    private void cancelSuggestionsWorker() {
        if (suggestionsWorker != null && !suggestionsWorker.isDone()) {
            suggestionsWorker.cancel(true);
        }
    }
}
