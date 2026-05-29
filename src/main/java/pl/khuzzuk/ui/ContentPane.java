package pl.khuzzuk.ui;

import pl.khuzzuk.Context;
import pl.khuzzuk.index.IndexItem;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.List;

public class ContentPane extends JPanel {
    private static final String TRACKS_CARD = "tracks";
    private static final String LOADING_CARD = "loading";
    private static final Color LOADING_DOT_COLOR = new Color(80, 130, 190);
    private static final Color LOADING_TEXT_COLOR = new Color(80, 80, 80);

    private final CardLayout tracksCardLayout = new CardLayout();
    private final JPanel tracksArea = new JPanel(tracksCardLayout);
    private final TracksTable tracksTable;

    public ContentPane(Context context, PlaylistPane playlist) {
        super(new GridBagLayout());

        this.tracksTable = new TracksTable(context, playlist::addTracks);
        tracksArea.add(new JScrollPane(tracksTable), TRACKS_CARD);
        tracksArea.add(new LoadingPanel(), LOADING_CARD);

        FileTree fileTree = new FileTree(context, this::showMappedFiles);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1.0;
        c.insets = new Insets(0, 5, 0, 5);

        c.gridx = 0;
        c.weightx = 0.2;
        add(fileTree, c);
        c.gridx = 1;
        c.weightx = 0.6;
        add(tracksArea, c);
        c.gridx = 2;
        c.weightx = 0.2;
        add(playlist, c);
    }

    private void showMappedFiles(List<IndexItem> files) {
        if (files == null || files.isEmpty()) {
            tracksTable.showMappedFiles(files);
            showTracks();
            return;
        }

        showLoading();
        tracksTable.showMappedFiles(files, this::showTracks);
    }

    private void showLoading() {
        tracksCardLayout.show(tracksArea, LOADING_CARD);
    }

    private void showTracks() {
        tracksCardLayout.show(tracksArea, TRACKS_CARD);
    }

    private static class LoadingPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String text = "Wczytywanie...";
            FontMetrics metrics = g.getFontMetrics();
            int dotSize = 10;
            int gap = 8;
            int textWidth = metrics.stringWidth(text);
            int totalWidth = dotSize + gap + textWidth;
            int x = Math.max(0, (getWidth() - totalWidth) / 2);
            int centerY = getHeight() / 2;
            int dotY = centerY - dotSize / 2;
            int textY = centerY + (metrics.getAscent() - metrics.getDescent()) / 2;

            g.setColor(LOADING_DOT_COLOR);
            g.fillOval(x, dotY, dotSize, dotSize);
            g.setColor(LOADING_TEXT_COLOR);
            g.drawString(text, x + dotSize + gap, textY);
            g.dispose();
        }
    }
}
