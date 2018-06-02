package com.barrybecker4.optimization.viewer1.projectors;

import com.barrybecker4.common.math.Range;
import com.barrybecker4.optimization.parameter1.ParameterArray;

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

    public Range getXRange(ParameterArray params) {
        return getRange(params, 0);
    }

    public Range getYRange(ParameterArray params) {
        return getRange(params, 1);
    }

    private Range getRange(ParameterArray params, int modulus) {
        double min = 0;
        double max = 0;

        for (int i=0; i<params.size(); i++) {
            if (i % 2 == modulus) {
                min += params.get(i).getMinValue();
                max += params.get(i).getMaxValue();
            }
        }
        return new Range(min, max);
    }

}
