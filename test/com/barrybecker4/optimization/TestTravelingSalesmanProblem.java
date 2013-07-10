/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization;

import com.barrybecker4.optimization.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizees.TravelingSalesmanProblem;
import com.barrybecker4.optimization.optimizees.TravelingSalesmanVariation;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

/**
 * @author Barry Becker
 */
public class TestTravelingSalesmanProblem extends OptimizerTestCase {


    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (TravelingSalesmanVariation variation : TravelingSalesmanVariation.values()) {

            OptimizeeProblem problem = new TravelingSalesmanProblem(variation);
            String logFile = LOG_FILE_HOME + "analytic_" + variation + "_optimization.txt";

            Optimizer optimizer = new Optimizer(problem, logFile);

            ParameterArray initialGuess = problem.getInitialGuess();
            verifyTest(optimizationType, problem, initialGuess, optimizer, problem.getFitnessRange(),
                    variation.getErrorTolerancePercent(optimizationType), variation.toString());
        }
    }

}
