// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.SubsetSumProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.SubsetSumVariation;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import org.junit.Test;

import java.util.EnumSet;

/**
 * The subset sum problem is NP-complete.
 * @author Barry Becker
 */
public class TestSubsetSumProblem extends OptimizerTestCase {

    @Test
    public void testBruteForce() {
        doTest(OptimizationStrategyType.BRUTE_FORCE);
    }


    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (SubsetSumVariation variation : EnumSet.of(SubsetSumVariation.NO_SOLUTION)) {

            OptimizeeProblem problem = new SubsetSumProblem(variation);
            verifyProblem(problem, variation, optimizationType);
        }
    }

}
