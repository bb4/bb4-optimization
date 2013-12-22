/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.common.util.FileUtil;
import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticFunctionProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticVariation;
import com.barrybecker4.optimization.viewer.OptimizerEvalFrame;

/**
 * This is a simple search space to test the optimization package.
 * The function we will try to maximize is one of the AnalyticFunction variants.
 * Shows the solution visually
 *
 * @author Barry Becker
 */
public class GraphAnalyticFunctionSolution {

    /** Place to put performance results from the tests */
    private static final String PERFORMANCE_LOG =
            FileUtil.getHomeDir() + "optimization/test/results/poly_optimization.txt";


    /**
     * This finds the solution for the above optimization problem.
     * Shows the path to the solution graphically.
     */
    public static void main(String[] args) {

        OptimizeeProblem[] testProblems = new OptimizeeProblem[AnalyticVariation.values().length];
        int i = 0;
        for (AnalyticVariation v : AnalyticVariation.values()) {
            testProblems[i++] = new AnalyticFunctionProblem(v);
        }

        new OptimizerEvalFrame(PERFORMANCE_LOG, testProblems);
    }
}
