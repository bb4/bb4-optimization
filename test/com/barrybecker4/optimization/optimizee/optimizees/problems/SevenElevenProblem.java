// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems;

import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter.NumericParameterArray;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import static com.barrybecker4.optimization.OptimizerTestCase.LOG_FILE_HOME;

/**
 * This is a simple search space to test the optimization package.
 * The problem we will try to solve is :
 *
 *   p1 + p2 + p3 + p4  = 711
 *   p1 * p2 * p3 * p4  = 711000000
 *
 * Which corresponds to the problem of someone going into a 7-11 and buying 4 things
 * whose sum and product equal $7.11.
 * This problem can be solved analytically by finding the prime factors of 711 and
 * eliminating combinations until you are left with:
 *   316, 125, 120, 150
 * as being the only solution.
 * Our choice of evaluation function to maximize is somewhat arbitrary.
 * When this function evaluates to 0, we have a solution.
 *
 * @see ParabolaMaxFunctionProblem for an easier optimization example.
 *
 * @author Barry Becker
 */
public class SevenElevenProblem extends OptimizeeProblem {

    private static final Parameter[] INITIAL_GUESS_PARAMS =  {
             new IntegerParameter(100, 0, 708, "p1"),
             new IntegerParameter(200, 0, 708, "p2"),
             new IntegerParameter(200, 0, 708, "p3"),
             new IntegerParameter(200, 0, 708, "p4")};

    private static final int  P1 = 316;
    private static final int  P2 = 125;
    private static final int  P3 = 120;
    private static final int  P4 = 150;
    /** these may be in any order, however */
    private static final Parameter[] EXACT_SOLUTION_PARAMS =  {
             new IntegerParameter(P1, 0, 708, "p1"),
             new IntegerParameter(P2, 0, 708, "p2"),
             new IntegerParameter(P3, 0, 708, "p3"),
             new IntegerParameter(P4, 0, 708, "p4")};

    private static final ParameterArray INITIAL_GUESS = new NumericParameterArray(INITIAL_GUESS_PARAMS);
    private static final ParameterArray EXACT_SOLUTION = new NumericParameterArray(EXACT_SOLUTION_PARAMS);

    // @@ exp errors.
    private static final double FITNESS_RANGE = 5000000.0;

    /** constructor */
    public SevenElevenProblem() {
    }

    /**
     * Evaluate directly, not by comparing with a different trial.
     */
    @Override
    public boolean evaluateByComparison() {
        return false;
    }

    @Override
    public String getName() {
        return "Seven Eleven Problem";
    }

    /**
     *  The choice of fitness function here is somewhat arbitrary.
     *  I chose to use:
     *    -bs( p1 + p2 + p3 + p4 - 711)^3  + abs(711000000 - p1 * p2 * p3 * p4)
     *    or
     *    abs(711 - sum) + abs(711000000 - product)/1000000
     *  This is 0 when the constraints are satisfied, something greater than 0 when not.
     *
     * @param a the position in the search space given values of p1, p2, p4, p4.
     * @return fitness value
     */
    @Override
    public double evaluateFitness(ParameterArray a) {

        double sum = a.get(0).getValue() + a.get(1).getValue() + a.get(2).getValue() + a.get(3).getValue();
        double product = a.get(0).getValue() * a.get(1).getValue() * a.get(2).getValue() * a.get(3).getValue();

        return Math.abs(711.0 - sum) + Math.abs(711000000.0 - product) / 1000000.0;
    }


    @Override
    public ParameterArray getExactSolution() {
        return EXACT_SOLUTION;
    }

    @Override
    public ParameterArray getInitialGuess() {
        return INITIAL_GUESS;
    }

    @Override
    public double getFitnessRange() {
        return FITNESS_RANGE;
    }

    /**
     * This finds the solution for the above optimization problem.
     */
    public static void main(String[] args)
    {
        OptimizeeProblem problem = new SevenElevenProblem();
        Optimizer optimizer =
                new Optimizer(problem, LOG_FILE_HOME + "seven11_optimization.txt");

        ParameterArray initialGuess = problem.getInitialGuess();

        ParameterArray solution =
                optimizer.doOptimization(OptimizationStrategyType.GLOBAL_SAMPLING, initialGuess, FITNESS_RANGE);

        showSolution(problem, solution);
    }
}
