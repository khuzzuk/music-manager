package pl.khuzzuk.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

class MetadataEditDialogModeler {
    private static final Color CHANGED_FIELD_COLOR = new Color(250, 235, 198);

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

    void modelEditor(JComponent editor) {
        editor.setFont(UiTheme.BODY_FONT);
        editor.setForeground(UiTheme.INK);
        editor.setBackground(UiTheme.SURFACE);
        editor.setBorder(UiTheme.lineBorder());
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
