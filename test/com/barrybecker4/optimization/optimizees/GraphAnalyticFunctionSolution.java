/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.optimizees;

import com.barrybecker4.common.util.FileUtil;
import com.barrybecker4.optimization.Optimizer;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;
import com.barrybecker4.optimization.viewer.OptimizerEvalFrame;

import javax.vecmath.Point2d;

/**
 * This is a simple search space to test the optimization package.
 * The function we will try to maximize is one of the AnalyticFunction variants.
 * Shows the solution visually
 *
 * @author Barry Becker
 */
public class GraphAnalyticFunctionSolution extends AnalyticFunctionProblem {

    /** Place to put performance results from the tests */
    private static final String PERFORMANCE_DIR =
            FileUtil.getHomeDir() + "optimization/test/results/poly_optimization.txt";

    /** Constructor */
    public GraphAnalyticFunctionSolution(AnalyticVariation v) {
        super(v);
    }

    /**
     * This finds the solution for the above optimization problem.
     * Shows the path to the solution graphically.
     */
    public static void main(String[] args) {
        AnalyticVariation v = AnalyticVariation.PARABOLA;
        OptimizeeProblem testProblem = new GraphAnalyticFunctionSolution(v);

        Optimizer optimizer = new Optimizer(testProblem, PERFORMANCE_DIR);

        Point2d solutionPosition = new Point2d(AnalyticFunctionConsts.P1, AnalyticFunctionConsts.P2);
        OptimizationStrategyType strategy = OptimizationStrategyType.GLOBAL_SAMPLING;

        new OptimizerEvalFrame(optimizer, solutionPosition, strategy, testProblem);
    }
}
