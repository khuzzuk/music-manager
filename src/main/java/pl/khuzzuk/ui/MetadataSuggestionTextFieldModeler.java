package pl.khuzzuk.ui;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Dimension;
import java.awt.FontMetrics;

class MetadataSuggestionTextFieldModeler {
    private static final int ROW_VERTICAL_PADDING = 7;
    private static final int ITEM_HORIZONTAL_PADDING = 32;
    private static final int MAX_POPUP_WIDTH = 640;

    void modelSuggestionsPopup(JPopupMenu suggestionsPopup) {
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBorder(UiTheme.lineBorder());
    }

    void modelSuggestionsScrollPane(JScrollPane suggestionsScrollPane) {
        suggestionsScrollPane.setBorder(UiTheme.empty(0, 0, 0, 0));
        suggestionsScrollPane.setViewportBorder(UiTheme.empty(0, 0, 0, 0));
        suggestionsScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        suggestionsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        UiTheme.modelScrollBar(suggestionsScrollPane.getVerticalScrollBar());
        UiTheme.modelScrollBar(suggestionsScrollPane.getHorizontalScrollBar());
    }

    void modelSuggestionsSize(
            JList<String> suggestionsList,
            JScrollPane suggestionsScrollPane,
            JPopupMenu suggestionsPopup,
            int anchorWidth,
            int maxVisibleRows) {
        suggestionsList.setFont(UiTheme.BODY_FONT);
        suggestionsList.setForeground(UiTheme.INK);
        suggestionsList.setBackground(UiTheme.SURFACE);
        suggestionsList.setSelectionBackground(UiTheme.SELECTION);
        suggestionsList.setSelectionForeground(UiTheme.INK);

        int rowCount = suggestionsList.getModel().getSize();
        int visibleRows = Math.clamp(rowCount, 1, maxVisibleRows);
        int rowHeight = rowHeight(suggestionsList);
        int width = Math.clamp(anchorWidth, preferredContentWidth(suggestionsList), MAX_POPUP_WIDTH);
        int height = visibleRows * rowHeight;

        suggestionsList.setVisibleRowCount(visibleRows);
        suggestionsList.setFixedCellHeight(rowHeight);
        suggestionsList.setPreferredSize(new Dimension(width, rowCount * rowHeight));
        suggestionsScrollPane.getViewport().setPreferredSize(new Dimension(width, height));
        suggestionsScrollPane.setPreferredSize(suggestionsScrollPane.getPreferredSize());
        suggestionsPopup.setPreferredSize(suggestionsScrollPane.getPreferredSize());
    }

    private int rowHeight(JList<String> suggestionsList) {
        FontMetrics metrics = suggestionsList.getFontMetrics(suggestionsList.getFont());
        return metrics.getHeight() + ROW_VERTICAL_PADDING;
    }

    private int preferredContentWidth(JList<String> suggestionsList) {
        FontMetrics metrics = suggestionsList.getFontMetrics(suggestionsList.getFont());
        int width = 0;
        for (int i = 0; i < suggestionsList.getModel().getSize(); i++) {
            String value = suggestionsList.getModel().getElementAt(i);
            width = Math.max(width, metrics.stringWidth(value == null ? "" : value));
        }

        return width + ITEM_HORIZONTAL_PADDING;
    }
}
