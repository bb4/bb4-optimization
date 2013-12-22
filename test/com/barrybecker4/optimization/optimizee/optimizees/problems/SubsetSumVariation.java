/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.optimizee.optimizees.problems;

import com.barrybecker4.optimization.optimizee.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee.optimizees.IProblemVariation;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.parameter.VariableLengthIntArray;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An enum for different sorts of subset sum problems.
 * http://en.wikipedia.org/wiki/Subset_sum_problem
 *
 * @author Barry Becker
 */
public enum SubsetSumVariation implements IProblemVariation {

    SIMPLE {
        private final ErrorTolerances ERROR_TOLERANCES = new ErrorTolerances(0.0, 0.0, 0.0, 0.0, 0.0, 17.0, 17.0, 0.0);

        protected List<Integer> getNumberSet() {
            return Arrays.asList(-7, -3, -2, 5, 8);
        }

        public ParameterArray getExactSolution() {
            return createSolution(-3, -2, 5);
        }

        @Override
        public double getFitnessRange() {
            return 12.0;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },

    TYPICAL {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.0, 0.0, 1.0, 6.0, 1.0, 1.0, 1.0, 1.0);

        protected List<Integer> getNumberSet() {
            return Arrays.asList(-7, -33, -21, 5, 83, -29, -78, 213, 123, -34, -37, -41, 91, 7, -17);
        }

        /** This is one of several possible solutions that gives an optimal fitness of 0 */
        public ParameterArray getExactSolution() {
            return createSolution(6, -6);
        }

        @Override
        public double getFitnessRange() {
            return 200.0;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    };

    /** @return the number of nodes in the graph */
    public int getNumElements() {
        return getNumberSet().size();
    }

    /** The graph containing the node adjacency information */
    protected abstract List<Integer> getNumberSet();

    /**
     * Some random initial set of marked nodes.
     * One half or one third of the nodes is probably a good starting point.
     */
    public ParameterArray getInitialGuess() {
        int num = this.getNumElements();
        List<Integer> numSet = this.getNumberSet();
        List<Parameter> params = new ArrayList<>(num);

        for (int i=0; i<num; i+=3) {
            params.add(createParam(numSet.get(i)));
        }
        VariableLengthIntArray pa = new VariableLengthIntArray(params, getNumberSet());
        pa.setFitness(computeCost(pa));
        return pa;
    }

    /**
     * Evaluate fitness for the analytics function.
     * @param pa param array
     * @return fitness value
     */
    public double evaluateFitness(ParameterArray pa) {
        return computeCost(pa);
    }

    /** Approximate value of maxCost - minCost */
    public abstract double getFitnessRange();

    /**
     * We assume that the parameter array contains 0 based integers
     * @param params last best guess at dominating set.
     * @return the total cost of the path represented by param.
     */
    protected double computeCost(ParameterArray params) {

        List<Integer> marked = new ArrayList<>();
        for (int i = 0; i < params.size(); i++)  {
            Parameter node = params.get(i);
            marked.add((int)node.getValue());
        }

        return getScore(marked);
    }

    public double getScore(List<Integer> marked) {
        int sum = 0;
        for (int i : marked) {
            sum += i;
        }
        return Math.abs(sum);
    }

    /** @return the error tolerance percent for a specific optimization strategy */
    public double getErrorTolerancePercent(OptimizationStrategyType opt) {
        return getErrorTolerances().getErrorTolerancePercent(opt);
    }

    /**
     * Error tolerance for each search strategy and variation of the problem.
     * @return error tolerance percent
     */
    protected abstract ErrorTolerances getErrorTolerances();

    /**
     * Create the solution based on the ordered list of cities.
     * @param nodeList optimal dominating set of marked nodes. May not be unique.
     * @return optimal solution (to compare against at the end of the test).
     */
    protected VariableLengthIntArray createSolution(int... nodeList) {
        int numNodes = nodeList.length;
        List<Parameter> params = new ArrayList<>(numNodes);
        for (int i : nodeList) {
            params.add(createParam(i));
        }
        return new VariableLengthIntArray(params, getNumberSet());
    }

    private Parameter createParam(int i) {
        return new IntegerParameter(i, 0, 100, "p" + i);
    }
}