/** Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.Optimizer;
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

    private DominatingSetVariation variation_ = DominatingSetVariation.SIMPLE;


    /** constructor */
    public DominatingSetProblem(DominatingSetVariation variation) {
        variation_ = variation;
    }

    @Override
    public String getName() {
        return "Dominating Set Problem";
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
        return variation_.evaluateFitness(a);
    }

    @Override
    public ParameterArray getExactSolution() {
        return variation_.getExactSolution();
    }

    @Override
    public ParameterArray getInitialGuess() {
        return variation_.getInitialGuess();
    }

    @Override
    public double getFitnessRange() {
        return variation_.getFitnessRange();
    }

    /**
     * This finds the solution for the above optimization problem.
     */
    public static void main(String[] args) {
        DominatingSetVariation v = DominatingSetVariation.SIMPLE;
        OptimizeeProblem problem = new DominatingSetProblem(v);
        Optimizer optimizer =
                new Optimizer(problem, LOG_FILE_HOME + "domSet_optimization.txt");

        ParameterArray initialGuess = problem.getInitialGuess();

        ParameterArray solution =
                optimizer.doOptimization(OptimizationStrategyType.SIMULATED_ANNEALING, initialGuess, v.getFitnessRange());

        showSolution(problem, solution);
    }
}
