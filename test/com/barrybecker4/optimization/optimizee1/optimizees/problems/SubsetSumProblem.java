// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.Optimizer1;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.parameter1.VariableLengthIntArray;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;
import static com.barrybecker4.optimization.OptimizerTestCase.LOG_FILE_HOME;

/**
 * Determining if a set of numbers has a subset that sums to 0 is NP-Complete.
 * The only strategy that is guaranteed to find a solution if it exists is brute force search.
 * http://en.wikipedia.org/wiki/Subset_sum_problem
 *
 * @author Barry Becker
 */
public class SubsetSumProblem extends OptimizeeProblem {

    private SubsetSumVariation variation;


    /** constructor */
    public SubsetSumProblem(SubsetSumVariation variation) {
        this.variation = variation;
    }

    @Override
    public String getName() {
        return "Subset Sum: " + variation.name();
    }

    /**
     * we evaluate directly not by comparing with a different trial.
     */
    @Override
    public boolean evaluateByComparison() {
        return false;
    }

    /**
     * Use the cost matrix for the TSP variation to determine this.
     * @return fitness value
     */
    @Override
    public double evaluateFitness(ParameterArray a) {
        return variation.evaluateFitness(a);
    }

    @Override
    public ParameterArray getExactSolution() {
        return variation.getExactSolution();
    }

    @Override
    public ParameterArray getInitialGuess() {
        return variation.getInitialGuess();
    }

    @Override
    public double getFitnessRange() {
        return variation.getFitnessRange();
    }

    /**
     * This finds the solution for the above optimization problem.
     */
    public static void main(String[] args) {
        OptimizeeProblem problem = new SubsetSumProblem(SubsetSumVariation.NO_SOLUTION);

        Optimizer1 optimizer =
                new Optimizer1(problem, LOG_FILE_HOME + "domSet_optimization.txt");

        ParameterArray initialGuess = problem.getInitialGuess();
        System.out.println("initial guess="+ initialGuess + " all=" + ((VariableLengthIntArray)initialGuess).getMaxLength());

        ParameterArray solution =
                optimizer.doOptimization(OptimizationStrategyType.GLOBAL_HILL_CLIMBING,
                        initialGuess, problem.getFitnessRange());

        showSolution(problem, solution);
    }
}
