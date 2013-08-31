/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.viewer.model;

import com.barrybecker4.common.math.Range;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.parameter.types.Parameter;
import com.barrybecker4.optimization.viewer.NavigationListener;

import javax.vecmath.Point2d;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for showing the optimization visually.
 * @author Barry Becker
 */
public class PointsList implements NavigationListener {


    /** zoom in increment */
    private static final double ZOOM_IN_INCREMENT = 0.2;

    /** zoom out by this much */
    private static final double ZOOM_OUT_INCREMENT = 1.05;

    /**
     * If -1, then pans like a camera (opposite camera direction);
     * if 1, then moves the scene in the direction of the arrow.
     */
    private static final int PAN_DIRECTION = -1;

    private List<Point2d> rawPoints_;

    private Point2d rawSolutionPosition_;
    private int edgeSize;
    private Range rangeX, rangeY;

    /**
     * Constructor
     * @param solutionPosition where we hope to wind up at.
     */
    public PointsList(Point2d solutionPosition, int edgeSize) {
        rawPoints_ = new ArrayList<>();
        rawSolutionPosition_ = solutionPosition;

        this.edgeSize = edgeSize;
    }

    public Point getSolutionPosition() {
        return new Point(getScaledXValue(rawSolutionPosition_.x),
                         getScaledYValue(rawSolutionPosition_.y));
    }

    public Point2d getRawPoint(int i) {
        return rawPoints_.get(i);
    }

    public Point getScaledPoint(int i) {
        Point2d pt = rawPoints_.get(i);
        return new Point(getScaledXValue(pt.x), getScaledYValue(pt.y));
        //return scaledPoints_.get(i);
    }

    public int size() {
        return rawPoints_.size();
    }

    /**
     * Called whenever the optimizer strategy moves incrementally toward the solution.
     * Does first time initialization.
     * @param params we assume there is only two.
     */
    public void addPoint(ParameterArray params) {

        Parameter xParam = params.get(0);
        Parameter yParam = (params.size() > 1) ? params.get(1):  params.get(0);

        if (rangeX == null) {
            rangeX = new Range(xParam.getMinValue(), xParam.getMaxValue());
            rangeY = new Range(yParam.getMinValue(), yParam.getMaxValue());
        }

        rawPoints_.add(new Point2d(xParam.getValue(), yParam.getValue()));
    }

    public void pan(Point2d offset) {
        adjustXRange(offset.x * rangeX.getExtent());
        adjustYRange(offset.y * rangeY.getExtent());
    }

    private void adjustXRange(double xOffset) {
        rangeX = new Range(rangeX.getMin() + xOffset, rangeX.getMax() + xOffset);
    }

    private void adjustYRange(double yOffset) {
        rangeY = new Range(rangeY.getMin() + yOffset, rangeY.getMax() + yOffset);
    }

    public void zoomIn() {
        double xOffset = 0.5 * ZOOM_IN_INCREMENT * rangeX.getExtent();
        double yOffset = 0.5 * ZOOM_IN_INCREMENT * rangeY.getExtent();
        adjustRanges(xOffset, yOffset);
    }

    public void zoomOut() {
        double xOffset = -0.5 * ZOOM_OUT_INCREMENT * rangeX.getExtent();
        double yOffset = -0.5 * ZOOM_OUT_INCREMENT * rangeY.getExtent();
        adjustRanges(xOffset, yOffset);
    }

    private void adjustRanges(double xOffset, double yOffset) {
        rangeX = new Range(rangeX.getMin() + xOffset, rangeX.getMax() - xOffset);
        rangeY = new Range(rangeY.getMin() + yOffset, rangeY.getMax() - yOffset);
    }

    private  int getScaledXValue(double value) {
        return (int) (edgeSize * (value - rangeX.getMin()) / rangeX.getExtent());
    }

    private  int getScaledYValue(double value) {
        return (int) (edgeSize * (value - rangeY.getMin()) / rangeY.getExtent());
    }
}