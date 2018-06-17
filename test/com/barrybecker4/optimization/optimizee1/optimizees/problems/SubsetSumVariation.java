// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.optimizee1.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee1.optimizees.IProblemVariation;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.parameter1.VariableLengthIntArray;
import com.barrybecker4.optimization.parameter1.distancecalculators.MagnitudeDistanceCalculator;
import com.barrybecker4.optimization.parameter1.types.IntegerParameter;
import com.barrybecker4.optimization.parameter1.types.Parameter;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An enum for different sorts of subset sum problems.
 * http://en.wikipedia.org/wiki/Subset_sum_problem
 *
 * This is a handcrafted dynamic programming solution
 * See http://www.geeksforgeeks.org/dynamic-programming-subset-sum-problem/
 *
 * // Returns true if there is a subset of set[] with sum equal to sum.
 * bool isSubsetSum(int set[], int n, int sum)
 * {
 *    if (sum == 0) return true;
 *    if (n == 0 && sum != 0)  return false;
 *
 *    // If last element is greater than sum, then ignore it
 *    if (set[n-1] > sum) return isSubsetSum(set, n-1, sum);
 *    // else, check if sum can be obtained by any of the following
 *    // (a) including the last element
 *    // (b) excluding the last element
 *    return isSubsetSum(set, n-1, sum) || isSubsetSum(set, n-1, sum-set[n-1]);
 * }
 *
 * call with isSubsetSum(set, set.size(), 0);
 *
 * @author Barry Becker
 */
public enum SubsetSumVariation implements IProblemVariation {

    SIMPLE_SS {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.5, 0.5, 8.0, 8.0, 0.0, 8.0, 8.0, 0.0);

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

    TYPICAL_SS {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.5, 0.5, 0.5, 1.0, 1.0, 4.0, 4.0, 0.0);

        protected List<Integer> getNumberSet() {
            return Arrays.asList(-7, -33, -21, 5, 83, -29, -78, 213, 123, -34, -37, -41, 91, -8, -17);
        }

        // This is one of several possible solutions that gives an optimal fitness of 0
        public ParameterArray getExactSolution() {
            return createSolution(-33, -21, 5, -29, 123, -37, -8);
        }

        @Override
        public double getFitnessRange() {
            return 210.0;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },

    NO_SOLUTION {
        // none of the errors will be 0 because there is no solution that sums to 0.
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(20.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        protected List<Integer> getNumberSet() {
            return Arrays.asList(-7, -33, -21, 5, -83, -29, -78, -113, -23, -34, -37, -41, -91, -9, -17);
        }

        /** There is no solution - i.e. no values that sum to 0. */
        public ParameterArray getExactSolution() {
            return createSolution(-7);
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
        VariableLengthIntArray pa =
                new VariableLengthIntArray(params, getNumberSet(), new MagnitudeDistanceCalculator());
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
     * We assume that the parameter array contains 0 based integers.
     * @param params last best guess at subset.
     * @return the total cost of the subset represented by param.
     * In this case the absolute sum of the marked values.
     */
    protected double computeCost(ParameterArray params) {
        int sum = 0;
        for (int i = 0; i < params.size(); i++)  {
            Parameter node = params.get(i);
            sum += (int)node.getValue();
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
     * @param numberList optimal dominating set of marked nodes. May not be unique.
     * @return optimal solution (to compare against at the end of the test).
     */
    protected VariableLengthIntArray createSolution(int... numberList) {
        int numNodes = numberList.length;
        assert numNodes>0 : "There must be some values in a valid solution.";
        List<Parameter> params = new ArrayList<>(numNodes);
        for (int i : numberList) {
            params.add(createParam(i));
        }
        return new VariableLengthIntArray(params, getNumberSet(), new MagnitudeDistanceCalculator());
    }

    private Parameter createParam(int i) {
        int min = i < 0 ? i : 0;
        int max = i >= 0 ? i : 0;
        return new IntegerParameter(i, min, max, "p" + i);
    }
}