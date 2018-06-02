package com.barrybecker4.optimization.viewer1.projectors;

import com.barrybecker4.common.math.Range;
import com.barrybecker4.optimization.parameter1.ParameterArray;

import javax.vecmath.Point2d;

/**
 * @author Barry Becker
 */
public interface IProjector {

    Point2d project(ParameterArray params);

    Range getXRange(ParameterArray params);

    Range getYRange(ParameterArray params);
}
