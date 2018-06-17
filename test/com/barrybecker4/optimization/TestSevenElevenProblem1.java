// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee1.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SevenElevenProblem;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

/**
 * @author Barry Becker
 */
public class TestSevenElevenProblem1 extends OptimizerTestCase1 {

    /** default error tolerance. */
    private static final double TOL = 0.006;

    /** the tolerances for each for the search strategies. */
    private static final ErrorTolerances ERROR_TOLERANCES =
            new ErrorTolerances(0.1, TOL/10.0, TOL/3.0, TOL/10.0, TOL, TOL/3.0, TOL/3.0, TOL);


    @Override
    protected void doTest(OptimizationStrategyType optType) {

       OptimizeeProblem problem = new SevenElevenProblem();
       Optimizer1 optimizer =
               new Optimizer1(problem, LOG_FILE_HOME + "sevenEleven_optimization.txt");

       ParameterArray initialGuess = problem.getInitialGuess();

       verifyTest(optType, problem, initialGuess, optimizer, problem.getFitnessRange(),
                  ERROR_TOLERANCES.getErrorTolerancePercent(optType), "Seven Eleven");
    }
}
