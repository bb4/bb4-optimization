// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.common.format.FormatUtil;
import com.barrybecker4.optimization.viewer.model.PointsList;

import javax.vecmath.Point2d;
import java.awt.*;

/**
 * Panel for showing the optimization visually.
 * @author Barry Becker
 */
public class PointsListRenderer {

    private static final int POINT_RADIUS = 7;
    private static final Color VECTOR_COLOR = new Color(10, 40, 255);

    public void render(PointsList points, Graphics2D  g2) {

        if (points.getSolutionPosition() != null)  {
            drawSolution(g2, points.getSolutionPosition());
        }

        for (int i=1; i < points.size(); i++) {
            drawVector(g2, points.getScaledPoint(i - 1), points.getScaledPoint(i), points.getRawPoint(i));
        }
    }

    private void drawVector(Graphics2D g2, Point lastPoint, Point currentPoint, Point2d rawPoint) {

        g2.setColor(VECTOR_COLOR);
        g2.drawLine(currentPoint.x, currentPoint.y, lastPoint.x, lastPoint.y);
        g2.setColor(Color.BLACK);
        g2.drawOval(currentPoint.x,  currentPoint.y, POINT_RADIUS, POINT_RADIUS);

        String label = "(" + FormatUtil.formatNumber(rawPoint.x) + ", " + FormatUtil.formatNumber(rawPoint.y) + ")";
        g2.drawString(label, currentPoint.x - 20, currentPoint.y + 6);
    }

    private void drawSolution(Graphics2D g2, Point position) {
        g2.setColor(Color.RED);
        drawOval(position, POINT_RADIUS - 2, g2);
        drawOval(position, POINT_RADIUS,     g2);
        drawOval(position, POINT_RADIUS + 4, g2);
        drawOval(position, POINT_RADIUS + 10, g2);
    }

    private void drawOval(Point position, int rad, Graphics2D g2) {
         g2.drawOval((int)(position.x - rad / 2.0),  (int)(position.y - rad /2.0),
                           rad, rad);
    }
}