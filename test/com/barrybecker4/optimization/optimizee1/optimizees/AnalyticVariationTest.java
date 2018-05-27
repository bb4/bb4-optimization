// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees;

import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaFunctionConsts;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaMinVariation;
import com.barrybecker4.optimization.parameter1.NumericParameterArrayTest;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import org.junit.Test;

import static com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaFunctionConsts.P1;
import static com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaFunctionConsts.P2;
import static org.junit.Assert.assertEquals;

/**
 * Verify that the minimum value of each variation is the same (1001).
 *
 * @author Barry Becker
 */
public class AnalyticVariationTest {

    protected static final double TOL = 0.0;

    @Test
    public void testVariationMaximum() {
        for (ParabolaMinVariation variant : ParabolaMinVariation.values()) {
            ParameterArray param = NumericParameterArrayTest.createParamArray(P1, P2);
            assertEquals("Unexpected maximum value for " + variant.toString(),
                    ParabolaFunctionConsts.EXACT_SOLUTION.getFitness(), variant.evaluateFitness(param), TOL);
        }
    }

}