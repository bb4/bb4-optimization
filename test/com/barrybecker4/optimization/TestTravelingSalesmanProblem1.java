// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.TravelingSalesmanProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.TravelingSalesmanVariation;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;
import org.junit.Test;

/**
 * @author Barry Becker
 */
public class TestTravelingSalesmanProblem1 extends OptimizerTestCase1 {

    @Test
    public void testBruteForce() {
        doTest(OptimizationStrategyType.BRUTE_FORCE);
    }

    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (TravelingSalesmanVariation variation : TravelingSalesmanVariation.values()) {

            OptimizeeProblem problem = new TravelingSalesmanProblem(variation);
            verifyProblem(problem, variation, optimizationType);
        }
    }

}
