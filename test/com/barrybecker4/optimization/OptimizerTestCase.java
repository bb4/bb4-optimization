// Copyright by Barry G. Becker, 2000-2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.common.util.FileUtil;
import com.barrybecker4.optimization.optimizee.optimizees.IProblemVariation;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Barry Becker
 */
public abstract class OptimizerTestCase  {

    /** Where the log files will go */
    public static final String LOG_FILE_HOME =
            FileUtil.getHomeDir() + "test/performance/test_optimizer/";  // NN_NLS

    /** If the error is this percent (or more) less than the error threshold, let the user know */
    private static final double THRESHOLD_SLACK_WARNING = 0.1;

    /** If the error is this percent less than the error threshold, give an error so the test can be updated */
    private static final double THRESHOLD_SLACK_ERROR = 0.2;

    /** If the error is less than this, then it is considered acceptable, even if off by a lot from the thresh */
    private static final double ACCEPTABLE_ERROR = 0.002;


    @Before
    public void setUp() {
        MathUtil.RANDOM().setSeed(0);
    }


    @Test
    public void testGlobalSampling() {
        doTest(OptimizationStrategyType.GLOBAL_SAMPLING);
    }

    @Test
    public void testSimulatedAnnealing() {

        doTest(OptimizationStrategyType.SIMULATED_ANNEALING);
    }

    @Test
    public void testGeneticSearch() {

        doTest(OptimizationStrategyType.GENETIC_SEARCH);
    }

    @Test
    public void testConcurrentGeneticSearch() {

        doTest(OptimizationStrategyType.CONCURRENT_GENETIC_SEARCH);
    }

    @Test
    public void testHillClimbing() {

        doTest(OptimizationStrategyType.HILL_CLIMBING);
    }

    @Test
    public void testGlobalHillClimbing() {

        doTest(OptimizationStrategyType.GLOBAL_HILL_CLIMBING);
    }

    /**
     * Run test for given optimization type
     * @param optType the optimization type to use.
     */
    protected abstract void doTest(OptimizationStrategyType optType);

    protected void verifyProblem(OptimizeeProblem problem, IProblemVariation variation,
                                 OptimizationStrategyType optType) {
        String logFile = LOG_FILE_HOME + "analytic_" + variation + "_optimization.txt";

        Optimizer optimizer = new Optimizer(problem, logFile);

        ParameterArray initialGuess = problem.getInitialGuess();

        double percent = variation.getErrorTolerancePercent(optType);
        verifyTest(optType, problem, initialGuess, optimizer, problem.getFitnessRange(),
                   percent, variation.toString());
    }

    /**
     * Give an error if not withing errorThresh of the exact solution.
     */
    protected static void verifyTest(OptimizationStrategyType optType,
                                     OptimizeeProblem problem,
                                     ParameterArray initialGuess,
                                     Optimizer optimizer, double fitnessRange,
                                     double errorThresh, String title) {

        System.out.println(title + "\nabout to apply "
                + optType + " to " + problem.getName() + " with initial guess = " + initialGuess + ".");

        ParameterArray solution = optimizer.doOptimization(optType, initialGuess, fitnessRange);

        double error = problem.getError(solution);
        assertTrue("*** " + title + " ***\nAllowable error exceeded using " + optType
                + ". \nError = " + error + "\n The Test Solution was " + solution
                + "\n but we expected to get something very close to the exact solution:\n "
                + problem.getExactSolution(),
                error <= errorThresh);

        if (error < (1.0 - THRESHOLD_SLACK_WARNING) * errorThresh && error > ACCEPTABLE_ERROR) {
            String message = "The error threshold of " + errorThresh + " for "
                    + optType + " is a bit slack. It could be reduced to " + error;
            System.out.println(message);
            assertFalse(message, error < (1.0 - THRESHOLD_SLACK_ERROR) * errorThresh);
        }

        System.out.println( "\n************************************************************************" );
        System.out.println( "The solution to the Problem using " + optType + " is :\n" + solution
            + "\nWhich evaluates to: " + optimizer.getOptimizee().evaluateFitness(solution) + " with error= " + error);
    }

}
