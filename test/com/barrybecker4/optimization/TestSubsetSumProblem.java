// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SubsetSumProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SubsetSumVariation;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;
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
