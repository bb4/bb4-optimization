// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.DominatingSetProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.DominatingSetVariation;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import org.junit.Test;

/**
 * @author Barry Becker
 */
public class TestDominatingSetProblem extends OptimizerTestCase {

    @Test
    public void testBruteForce() {
        doTest(OptimizationStrategyType.BRUTE_FORCE);
    }


    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (DominatingSetVariation variation : DominatingSetVariation.values()) {

            OptimizeeProblem problem = new DominatingSetProblem(variation);
            verifyProblem(problem, variation, optimizationType);
        }
    }

}
