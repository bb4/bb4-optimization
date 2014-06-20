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

    private static final int POINT_DIAMETER = 8;
    private static final Color VECTOR_COLOR = new Color(10, 40, 255);
    private static final Color POINT_COLOR = new Color(10, 0, 55);
    private static final Color FINAL_POINT_COLOR = new Color(255, 80, 0);
    private static final Color SOLUTION_COLOR = new Color(220, 0, 0);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);

    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);
    private static final BasicStroke POINT_STROKE = new BasicStroke(2.0f);

    public void render(PointsList points, Graphics2D  g2) {

        if (points.getSolutionPosition() != null)  {
            drawSolution(g2, points.getSolutionPosition());
        }

        int numPoints = points.size();
        for (int i=1; i < numPoints; i++) {
            drawVector(g2,
                points.getScaledPoint(i - 1), points.getScaledPoint(i), points.getRawPoint(i), (i == (numPoints-1)));
        }
    }

    private void drawVector(Graphics2D g2, Point lastPoint, Point currentPoint, Point2d rawPoint, boolean isLast) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setStroke(LINE_STROKE);
        g2.setColor(VECTOR_COLOR);
        g2.drawLine(currentPoint.x, currentPoint.y, lastPoint.x, lastPoint.y);

        g2.setStroke(POINT_STROKE);
        g2.setColor(isLast ? FINAL_POINT_COLOR : POINT_COLOR);
        drawOval(currentPoint, POINT_DIAMETER, g2);
        //g2.drawOval(currentPoint.x - POINT_RADIUS,  currentPoint.y - POINT_RADIUS, POINT_DIAMETER, POINT_DIAMETER);

        g2.setColor(TEXT_COLOR);
        String label = "(" + FormatUtil.formatNumber(rawPoint.x) + ", " + FormatUtil.formatNumber(rawPoint.y) + ")";
        g2.drawString(label, currentPoint.x - 20, currentPoint.y + 12);
    }

    private void drawSolution(Graphics2D g2, Point position) {
        g2.setColor(SOLUTION_COLOR);
        g2.setStroke(POINT_STROKE);
        drawOval(position, POINT_DIAMETER - 2, g2);
        drawOval(position, POINT_DIAMETER,     g2);
        drawOval(position, POINT_DIAMETER + 3, g2);
        drawOval(position, POINT_DIAMETER + 10, g2);
    }

    private void drawOval(Point position, int rad, Graphics2D g2) {
         g2.drawOval((int)(position.x - rad / 2.0),  (int)(position.y - rad /2.0),
                           rad, rad);
    }
}