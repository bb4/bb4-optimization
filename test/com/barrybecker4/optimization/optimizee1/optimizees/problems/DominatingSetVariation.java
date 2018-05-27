// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.optimizee1.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee1.optimizees.IProblemVariation;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.parameter.VariableLengthIntArray;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An enum for different sorts of dominating set problems.
 * http://en.wikipedia.org/wiki/Dominating_set
 *
 * @author Barry Becker
 */
public enum DominatingSetVariation implements IProblemVariation {

    SIMPLE_DS {
        /**
         * Trivial example.
         * There are three nodes, A, B, C. This list of lists defines the connectivity of the graph.
         */
        private final Graph ADJACENCIES = new Graph(
                Arrays.asList(1, 2),
                Arrays.asList(0, 2),
                Arrays.asList(0, 1)
        );

        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        protected Graph getAdjacencies() {
            return ADJACENCIES;
        }

        public ParameterArray getExactSolution() {
            return createSolution(0);
        }

        @Override
        public double getFitnessRange() {
            return 7.0;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },

    TYPICAL_DS {
        private final Graph ADJACENCIES = new Graph(
                Arrays.asList(15, 21, 25),
                Arrays.asList(2, 4, 7),
                Arrays.asList(1, 3, 5, 7),
                Arrays.asList(2, 5, 8, 9),
                Arrays.asList(1, 6, 12),        // 4
                Arrays.asList(2, 3, 8, 13),
                Arrays.asList(4, 10, 11, 12),
                Arrays.asList(1, 2, 12, 13),
                Arrays.asList(3, 5, 9, 14),     // 8
                Arrays.asList(3, 8, 15),
                Arrays.asList(6, 11, 18),
                Arrays.asList(6, 10, 16),
                Arrays.asList(4, 6, 7, 16, 17), // 12
                Arrays.asList(5, 7, 14, 17),
                Arrays.asList(8, 13, 15, 17),
                Arrays.asList(0, 9, 14, 21),
                Arrays.asList(11, 12, 19),      // 16
                Arrays.asList(12, 13, 14, 20, 21),
                Arrays.asList(10, 19, 22, 24),
                Arrays.asList(16, 18, 20),
                Arrays.asList(17, 19, 22, 23),  // 20
                Arrays.asList(0, 15, 17, 23),
                Arrays.asList(18, 20, 23, 24),
                Arrays.asList(20, 21, 22, 25),
                Arrays.asList(18, 22, 25),      // 24
                Arrays.asList(0, 23, 24)
        );

        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(2.5, 0.5, 1.0, 2.1, 1.0, 2.1, 2.1, 1.0, 0);

        protected Graph getAdjacencies() {
            return ADJACENCIES;
        }

        /** This is one of several possible solutions that gives an optimal fitness of 0 */
        public ParameterArray getExactSolution() {
            return createSolution(6, 7, 8, 19, 21, 24);
        }

        @Override
        public double getFitnessRange() {
            return 50.0;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    };

    /** @return the number of nodes in the graph */
    public List<Integer> getAllNodes() {
        int num = getAdjacencies().size();
        List<Integer> nodes = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            nodes.add(i);
        }
        return nodes;
    }

    /** The graph containing the node adjacency information */
    protected abstract Graph getAdjacencies();

    /**
     * Some random initial set of marked nodes.
     * One half or one third of the nodes is probably a good starting point.
     */
    public ParameterArray getInitialGuess() {
        int num = getAllNodes().size();
        List<Parameter> params = new ArrayList<>(num);
        // just add some of the nodes
        for (int i = 0; i < num; i += 3) {
            params.add(new IntegerParameter(i, 0, num - 1, "p" + i));
        }
        VariableLengthIntArray pa = VariableLengthIntArray.createInstance(params, getAllNodes());
        pa.setFitness(getScore(getMarked(pa)));
        return pa;
    }

    private List<Integer> getMarked(VariableLengthIntArray pa) {
        List<Integer> marked = new ArrayList<>(pa.size());
        for (int i = 0; i < pa.size(); i++) {
           marked.add((int)pa.get(i).getValue());
        }
        return marked;
    }

    /**
     * Evaluate fitness for the candidate solution to the dominating set problem.
     * @param pa param array
     * @return fitness value. The closer it is to 0 the better. When 0 it is the exact cover.
     */
    public double evaluateFitness(ParameterArray pa) {
        return computeCost(pa) - getExactSolution().size();
    }

    /** Approximate value of maxCost - minCost */
    public abstract double getFitnessRange();

    /**
     * We assume that the parameter array contains 0 based integers.
     * @param params last best guess at dominating set.
     * @return the total cost of the candidate vertex cover.
     *  It is defined as the number of nodes in the cover + the number of nodes not within 1 hop from that set.
     */
    protected double computeCost(ParameterArray params) {

        List<Integer> marked = new ArrayList<>();
        for (int i = 0; i < params.size(); i++)  {
            Parameter node = params.get(i);
            marked.add((int)node.getValue());
        }

        return getScore(marked);
    }

    private double getScore(List<Integer> marked) {
        return marked.size() + 0.4 * getAdjacencies().getNumNotWithinOneHop(marked);
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
        List<Integer> allNodes = getAllNodes();
        for (int i=0; i < nodeList.length; i++) {
            params.add(new IntegerParameter(nodeList[i], 0, allNodes.size() - 1, "p" + i));
        }
        return VariableLengthIntArray.createInstance(params, allNodes);
    }
}