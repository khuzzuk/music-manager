package pl.khuzzuk.ui;

import pl.khuzzuk.ui.icons.RatingIcon;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RatingEditor extends JComponent {
    private static final int MAX_RATING = 10;
    private static final int PADDING = 4;
    private int rating;
    private int previewRating = -1;

    public RatingEditor(int rating) {
        this.rating = Math.clamp(rating, 0, MAX_RATING);
        RatingIcon icon = new RatingIcon(MAX_RATING);
        setPreferredSize(new Dimension(icon.getIconWidth() + PADDING * 2, icon.getIconHeight() + PADDING * 2));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addKeyListener(new RatingKeyHandler());
    }

    public int getRating() {
        return rating;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        RatingIcon icon = new RatingIcon(previewRating >= 0 ? previewRating : rating);
        icon.paintIcon(this, graphics, PADDING, PADDING);
    }

    private void setRating(int rating) {
        this.rating = Math.clamp(rating, 0, MAX_RATING);
        repaint();
    }

    private int ratingFromMouseX(int x) {
        int iconWidth = new RatingIcon(MAX_RATING).getIconWidth();
        int localX = Math.clamp(x - PADDING, 0, iconWidth);
        if (localX == 0) {
            return 0;
        }

        return Math.clamp((int) Math.ceil(localX * 10.0 / iconWidth), 0, MAX_RATING);
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent event) {
            setPreviewRating(ratingFromMouseX(event.getX()));
        }

        @Override
        public void mousePressed(MouseEvent event) {
            requestFocusInWindow();
            setRating(ratingFromMouseX(event.getX()));
        }

        @Override
        public void mouseExited(MouseEvent event) {
            clearPreviewRating();
        }
    }

    private void setPreviewRating(int rating) {
        int nextPreviewRating = Math.clamp(rating, 0, MAX_RATING);
        if (previewRating == nextPreviewRating) {
            return;
        }

        previewRating = nextPreviewRating;
        repaint();
    }

    private void clearPreviewRating() {
        if (previewRating < 0) {
            return;
        }

        previewRating = -1;
        repaint();
    }

    private class RatingKeyHandler extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_LEFT || event.getKeyCode() == KeyEvent.VK_DOWN) {
                setRating(rating - 1);
            } else if (event.getKeyCode() == KeyEvent.VK_RIGHT || event.getKeyCode() == KeyEvent.VK_UP) {
                setRating(rating + 1);
            } else if (event.getKeyCode() == KeyEvent.VK_DELETE || event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                setRating(0);
            }
        }
    }
}
