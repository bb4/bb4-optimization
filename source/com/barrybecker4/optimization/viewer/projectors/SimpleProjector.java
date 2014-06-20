package com.barrybecker4.optimization.viewer.projectors;

import com.barrybecker4.optimization.parameter.ParameterArray;

import javax.vecmath.Point2d;

/**
 * This simple projector strategy simply adds the even dimension values together to get an x coordinate,
 * and adds the y dimension values together to get a y coordinate.
 *
 * @author Barry Becker
 */
public class SimpleProjector implements IProjector {

    public Point2d project(ParameterArray params) {

        double xVal = 0;
        double yVal = 0;

        for (int i=0; i<params.size(); i++) {
            double v = params.get(i).getValue();
            if (i % 2 == 0) {
                xVal += v;
            }
            else {
                yVal += v;
            }
        }
        return new Point2d(xVal, yVal);
    }
}
