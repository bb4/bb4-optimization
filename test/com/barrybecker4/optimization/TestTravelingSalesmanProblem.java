// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.TravelingSalesmanProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.TravelingSalesmanVariation;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import org.junit.Test;

/**
 * @author Barry Becker
 */
public class TestTravelingSalesmanProblem extends OptimizerTestCase {

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
