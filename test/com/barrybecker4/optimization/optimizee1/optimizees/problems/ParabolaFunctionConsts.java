// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.parameter.NumericParameterArray;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.parameter.types.DoubleParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;

/**
 * Constants related to the analytics functions
 *
 * @author Barry Becker
 */
public class ParabolaFunctionConsts {

    public static final double  P1 = 1.0;
    public static final double  P2 = 2.0;

    private static final Parameter[] EXACT_SOLUTION_PARAMS =
            {new DoubleParameter(P1, 0.0, 3.0, "p1"),
             new DoubleParameter(P2, 0.0, 3.0, "p2")};

    public static final ParameterArray EXACT_SOLUTION = new NumericParameterArray(EXACT_SOLUTION_PARAMS);
    static {
        EXACT_SOLUTION.setFitness(0.0);
    }

    // define the initialGuess in some bounded region of the 2-dimensional search space.
    private static final double[] VALUES = {  6.81,  7.93};   // initialGuess
    private static final double[] MIN_VALS = {-10.0, -10.0};
    private static final double[] MAX_VALS = { 10.0,  10.0};
    private static final String[] PARAM_NAMES = {"p1",   "p2"};
    public static final ParameterArray INITIAL_GUESS = new NumericParameterArray(VALUES, MIN_VALS, MAX_VALS, PARAM_NAMES);

    public static final double BASE_TOLERANCE = 0.0002;
    public static final double RELAXED_TOL = 0.0032;
    /** Really relax this one because we do not expect it to ever get that close */
    public static final double GLOB_SAMP_TOL = 0.03;

    private ParabolaFunctionConsts() {}
}
