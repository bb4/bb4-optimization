// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import static com.barrybecker4.optimization.OptimizerTestCase.LOG_FILE_HOME;

/**
 * See these references to help understand the problem of finding dominating sets given a graph.
 * http://csunplugged.org/dominating-sets (this is where I got the inspiration to add this class)
 * http://en.wikipedia.org/wiki/Dominating_set
 * This is a simple search example to help test the optimization package.
 *
 * @author Barry Becker
 */
public class DominatingSetProblem extends OptimizeeProblem {

    private DominatingSetVariation variation;


    /** constructor */
    public DominatingSetProblem(DominatingSetVariation variation) {
        this.variation = variation;
    }

    @Override
    public String getName() {
        return "Dominating Set: " + variation.name();
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
        DominatingSetVariation v = DominatingSetVariation.TYPICAL_DS;
        OptimizeeProblem problem = new DominatingSetProblem(v);
        Optimizer optimizer =
                new Optimizer(problem, LOG_FILE_HOME + "domSet_optimization.txt");

        ParameterArray initialGuess = problem.getInitialGuess();

        ParameterArray solution = optimizer.doOptimization(
            OptimizationStrategyType.SIMULATED_ANNEALING, initialGuess, v.getFitnessRange());

        showSolution(problem, solution);
    }
}
