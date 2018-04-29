// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.model;

import com.barrybecker4.common.math.Range;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.viewer.NavigationListener;
import com.barrybecker4.optimization.viewer.projectors.IProjector;

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

    private List<Point2d> rawPoints_;
    private List<ParameterArray> paramArrays_;

    private Point2d rawSolutionPosition_;
    private int edgeSize;
    private Range rangeX, rangeY;
    private IProjector projector;

    /**
     * Constructor
     * @param solutionPosition where we hope to wind up at.
     */
    public PointsList(Point2d solutionPosition, int edgeSize, IProjector projector) {
        rawPoints_ = new ArrayList<>();
        paramArrays_ = new ArrayList<>();
        rawSolutionPosition_ = solutionPosition;

        this.edgeSize = edgeSize;
        this.projector = projector;
    }

    public Point getSolutionPosition() {
        return new Point(getScaledXValue(rawSolutionPosition_.x),
                         getScaledYValue(rawSolutionPosition_.y));
    }

    public Point2d getRawPoint(int i) {
        return rawPoints_.get(i);
    }

    public ParameterArray getParamArrayForPoint(int i) {
        return paramArrays_.get(i);
    }

    public Point getScaledPoint(int i) {
        Point2d pt = rawPoints_.get(i);
        return new Point(getScaledXValue(pt.x), getScaledYValue(pt.y));
    }

    public int size() {
        return rawPoints_.size();
    }

    /**
     * Called whenever the optimizer strategy moves incrementally toward the solution.
     * Does first time initialization.
     * @param params the parameter array to add to the list.
     */
    public void addPoint(ParameterArray params) {

        if (rangeX == null) {
            rangeX = projector.getXRange(params);
            rangeY = projector.getYRange(params);
        }

        rawPoints_.add(projector.project(params));
        paramArrays_.add(params);
    }

    public void pan(Point2d offset) {
        adjustXRange(offset.x * rangeX.getExtent());
        adjustYRange(offset.y * rangeY.getExtent());
    }

    private void adjustXRange(double xOffset) {
        rangeX = new Range(rangeX.min() + xOffset, rangeX.max() + xOffset);
    }

    private void adjustYRange(double yOffset) {
        rangeY = new Range(rangeY.min() + yOffset, rangeY.max() + yOffset);
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
        rangeX = new Range(rangeX.min() + xOffset, rangeX.max() - xOffset);
        rangeY = new Range(rangeY.min() + yOffset, rangeY.max() - yOffset);
    }

    private  int getScaledXValue(double value) {
        if (rangeX == null) return 0;
        return (int) (edgeSize * (value - rangeX.min()) / rangeX.getExtent());
    }

    private  int getScaledYValue(double value) {
        if (rangeY == null) return 0;
        return (int) (edgeSize * (value - rangeY.min()) / rangeY.getExtent());
    }
}