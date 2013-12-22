/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee.optimizees.problems.DominatingSetProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.DominatingSetVariation;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter.ParameterArray;
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
            String logFile = LOG_FILE_HOME + "analytic_" + variation + "_optimization.txt";

            Optimizer optimizer = new Optimizer(problem, logFile);

            ParameterArray initialGuess = problem.getInitialGuess();
            verifyTest(optimizationType, problem, initialGuess, optimizer, problem.getFitnessRange(),
                    variation.getErrorTolerancePercent(optimizationType), variation.toString());
        }
    }

}
