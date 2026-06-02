package pl.khuzzuk.ui;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import java.awt.Dimension;

class MetadataSuggestionTextFieldModeler {
    void modelSuggestionsPopup(JPopupMenu suggestionsPopup) {
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBorder(UiTheme.lineBorder());
    }

    void modelSuggestionsSize(JList<String> suggestionsList, JPopupMenu suggestionsPopup, int width) {
        suggestionsList.setFont(UiTheme.BODY_FONT);
        suggestionsList.setForeground(UiTheme.INK);
        suggestionsList.setBackground(UiTheme.SURFACE);
        suggestionsList.setSelectionBackground(UiTheme.SELECTION);
        suggestionsList.setSelectionForeground(UiTheme.INK);
        suggestionsList.setPreferredSize(
                new Dimension(width, suggestionsList.getPreferredScrollableViewportSize().height));
        suggestionsPopup.setPreferredSize(new Dimension(width, suggestionsPopup.getPreferredSize().height));
    }
}
