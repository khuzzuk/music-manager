package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;

class MetadataEditDialogModeler {
    private static final Color CHANGED_FIELD_COLOR = new Color(250, 235, 198);
    private static final Color LOCKED_FIELD_BACKGROUND = UiTheme.SURFACE_ALT;
    private static final Color LOCKED_FIELD_BORDER = new Color(208, 201, 188);
    private static final Color LOCKED_FIELD_TEXT = UiTheme.MUTED_INK;

    void modelDialog(JDialog dialog) {
        dialog.getContentPane().setBackground(UiTheme.BACKGROUND);
        dialog.setMinimumSize(new Dimension(520, 420));
    }

    void modelForm(JPanel form) {
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    }

    Insets formFieldInsets() {
        return new Insets(4, 4, 4, 4);
    }

    void modelFormScrollPane(JScrollPane scrollPane) {
        UiTheme.modelScrollPane(scrollPane);
    }

    void modelFieldLabel(JLabel label) {
        label.setFont(UiTheme.BODY_BOLD_FONT);
        label.setForeground(UiTheme.ACCENT_DARK);
    }

    void modelPathLabel(JLabel label) {
        label.setFont(UiTheme.BODY_FONT);
        label.setForeground(UiTheme.MUTED_INK);
        label.setBorder(UiTheme.empty(0, 4, 10, 4));
    }

    void modelEditor(JComponent editor) {
        editor.setFont(UiTheme.BODY_FONT);
        editor.setForeground(UiTheme.INK);
        editor.setBackground(UiTheme.SURFACE);
        editor.setBorder(UiTheme.lineBorder());
    }

    void modelFieldState(JLabel label, JComponent editor, boolean editable) {
        label.setForeground(editable ? UiTheme.ACCENT_DARK : UiTheme.MUTED_INK);
        editor.setForeground(editable ? UiTheme.INK : LOCKED_FIELD_TEXT);
        editor.setBackground(editable ? UiTheme.SURFACE : LOCKED_FIELD_BACKGROUND);
        editor.setBorder(BorderFactory.createLineBorder(editable ? UiTheme.BORDER : LOCKED_FIELD_BORDER));
        editor.setCursor(Cursor.getPredefinedCursor(editable ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
        editor.setOpaque(true);

        if (editor instanceof JTextComponent textComponent) {
            textComponent.setDisabledTextColor(LOCKED_FIELD_TEXT);
            textComponent.setCaretColor(editable ? UiTheme.ACCENT_DARK : LOCKED_FIELD_BACKGROUND);
        } else if (editor instanceof RatingEditor) {
            editor.setCursor(Cursor.getPredefinedCursor(editable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }

    void modelButtonsPanel(JPanel panel) {
        panel.setBackground(UiTheme.BACKGROUND);
        panel.setBorder(UiTheme.empty(4, 12, 12, 12));
    }

    void modelCancelButton(JButton button) {
        UiTheme.modelSecondaryButton(button);
    }

    void modelSaveButton(JButton button) {
        UiTheme.modelPrimaryButton(button);
    }

    void modelEditorChangeState(JComponent editor, boolean changed, Color defaultBackground) {
        editor.setBackground(changed ? CHANGED_FIELD_COLOR : defaultBackground);
    }
}
