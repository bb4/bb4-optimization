// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticFunctionConsts;
import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticVariation;
import com.barrybecker4.optimization.parameter.NumericParameterArrayTest;
import com.barrybecker4.optimization.parameter.ParameterArray;
import org.junit.Test;

import static com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticFunctionConsts.P1;
import static com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticFunctionConsts.P2;
import static org.junit.Assert.assertEquals;

/**
 * Verify that the maximum value of each variation is the same (1001).
 *
 * @author Barry Becker
 */
public class AnalyticVariationTest {

    protected static final double TOL = 0.0;

    @Test
    public void testVariationMaximum() {
        for (AnalyticVariation variant : AnalyticVariation.values()) {
            ParameterArray param = NumericParameterArrayTest.createParamArray(P1, P2);
            assertEquals("Unexpected maximum value for " + variant.toString(),
                    AnalyticFunctionConsts.EXACT_SOLUTION.getFitness(), variant.evaluateFitness(param), TOL);
        }
    }

}