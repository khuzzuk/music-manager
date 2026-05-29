package pl.khuzzuk.ui.icons;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;

public class RatingIcon implements Icon {
    private static final int MAX_RATING = 10;
    private static final int STAR_COUNT = 5;
    private static final int STAR_SIZE = 14;
    private static final int STAR_GAP = 2;
    private static final Color FILLED_COLOR = new Color(245, 174, 35);
    private static final Color EMPTY_COLOR = new Color(185, 185, 185);
    private final int rating;

    public RatingIcon(int rating) {
        this.rating = Math.clamp(rating, 0, MAX_RATING);
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.2f));

        for (int i = 0; i < STAR_COUNT; i++) {
            int starX = x + i * (STAR_SIZE + STAR_GAP);
            int starRating = rating - i * 2;
            paintStar(g, starX, y, starRating);
        }

        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return STAR_COUNT * STAR_SIZE + (STAR_COUNT - 1) * STAR_GAP;
    }

    @Override
    public int getIconHeight() {
        return STAR_SIZE;
    }

    private void paintStar(Graphics2D g, int x, int y, int starRating) {
        Shape star = createStar(x + STAR_SIZE / 2.0, y + STAR_SIZE / 2.0);
        g.setColor(EMPTY_COLOR);
        g.draw(star);

        if (starRating <= 0) {
            return;
        }

        Shape previousClip = g.getClip();
        if (starRating == 1) {
            g.clipRect(x, y, STAR_SIZE / 2, STAR_SIZE);
        }

        g.setColor(FILLED_COLOR);
        g.fill(star);
        g.setColor(FILLED_COLOR.darker());
        g.draw(star);

        if (starRating == 1) {
            g.setClip(previousClip);
            g.setColor(EMPTY_COLOR);
            g.draw(star);
        }
    }

    private Shape createStar(double centerX, double centerY) {
        double outerRadius = STAR_SIZE / 2.0 - 1;
        double innerRadius = outerRadius * 0.45;
        Path2D.Double path = new Path2D.Double();

        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-90 + i * 36);
            double radius = i % 2 == 0 ? outerRadius : innerRadius;
            double pointX = centerX + Math.cos(angle) * radius;
            double pointY = centerY + Math.sin(angle) * radius;
            if (i == 0) {
                path.moveTo(pointX, pointY);
            } else {
                path.lineTo(pointX, pointY);
            }
        }

        path.closePath();
        return path;
    }
}
