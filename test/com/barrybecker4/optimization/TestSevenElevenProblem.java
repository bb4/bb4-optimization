/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizee.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.SevenElevenProblem;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

/**
 * @author Barry Becker
 */
public class TestSevenElevenProblem extends OptimizerTestCase {

    /** default error tolerance. */
    private static final double TOL = 0.006;

    /** the tolerances for each for the search strategies. */
    private static final ErrorTolerances ERROR_TOLERANCES =
            new ErrorTolerances(0.1, TOL/10.0, TOL/3.0, TOL/30.0, TOL, TOL/3.0, TOL/3.0, TOL);


    @Override
    protected void doTest(OptimizationStrategyType optType) {

       OptimizeeProblem problem = new SevenElevenProblem();
       Optimizer optimizer =
               new Optimizer(problem, LOG_FILE_HOME + "sevenEleven_optimization.txt");

       ParameterArray initialGuess = problem.getInitialGuess();

       verifyTest(optType, problem, initialGuess, optimizer, problem.getFitnessRange(),
                  ERROR_TOLERANCES.getErrorTolerancePercent(optType), "Seven Eleven");
    }
}
