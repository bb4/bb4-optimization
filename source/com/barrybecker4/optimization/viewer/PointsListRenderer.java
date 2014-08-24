// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.viewer.model.PointsList;
import com.barrybecker4.ui.util.ColorMap;

import javax.vecmath.Point2d;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 * Panel for showing the optimization visually.
 * @author Barry Becker
 */
public class PointsListRenderer {

    private static final int POINT_DIAMETER = 8;
    private static final Color VECTOR_COLOR = new Color(10, 20, 255, 120);
    private static final Color POINT_COLOR = new Color(10, 0, 55);
    private static final Color FINAL_POINT_COLOR = new Color(255, 80, 0);
    private static final Color SOLUTION_COLOR = new Color(220, 0, 0);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);

    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);
    private static final BasicStroke POINT_STROKE = new BasicStroke(2.0f);

    private static final ColorMap cmap = new FitnessColorMap();

    public void render(PointsList points, Graphics2D  g2) {

        if (points.getSolutionPosition() != null)  {
            drawSolution(g2, points.getSolutionPosition());
        }

        int numPoints = points.size();
        for (int i=1; i < numPoints; i++) {
            drawVector(g2,
                points.getScaledPoint(i - 1), points.getScaledPoint(i), points.getRawPoint(i),
                    points.getParamArrayForPoint(i), i == (numPoints-1));
        }
    }

    private void drawVector(Graphics2D g2,
        Point lastPoint, Point currentPoint, Point2d rawPoint, ParameterArray params, boolean isLast) {

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(LINE_STROKE);
        g2.setColor(VECTOR_COLOR);
        g2.drawLine(currentPoint.x, currentPoint.y, lastPoint.x, lastPoint.y);

        g2.setColor(cmap.getColorForValue(params.getFitness()));
        fillOval(currentPoint, 3* POINT_DIAMETER, g2);

        g2.setStroke(POINT_STROKE);
        g2.setColor(isLast ? FINAL_POINT_COLOR : POINT_COLOR);
        drawOval(currentPoint, POINT_DIAMETER, g2);

        g2.setColor(TEXT_COLOR);
        //String label = "(" + FormatUtil.formatNumber(rawPoint.x) + ", " + FormatUtil.formatNumber(rawPoint.y) + ")";

        String label = "(" + params.toCSVString() + ")";
        g2.drawString(label, currentPoint.x - 10 - 5 * label.length(), currentPoint.y + 12);
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

    private void fillOval(Point position, int rad, Graphics2D g2) {
         g2.fillOval((int)(position.x - rad / 2.0),  (int)(position.y - rad /2.0),
                     rad, rad);
    }
}