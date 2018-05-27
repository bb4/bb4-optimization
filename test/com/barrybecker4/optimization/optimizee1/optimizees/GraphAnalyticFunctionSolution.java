// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees;

import com.barrybecker4.common.util.FileUtil;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaMinFunctionProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaMinVariation;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.DominatingSetProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.DominatingSetVariation;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SevenElevenProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SubsetSumProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.SubsetSumVariation;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.TravelingSalesmanProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.TravelingSalesmanVariation;
import com.barrybecker4.optimization.viewer.OptimizerEvalFrame;

import java.util.LinkedList;
import java.util.List;

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
            FileUtil.getHomeDir() + "test/results/poly_optimization.txt";


    /**
     * This finds the solution for the above optimization problem.
     * Shows the path to the solution graphically.
     */
    public static void main(String[] args) {

        new OptimizerEvalFrame(PERFORMANCE_LOG, getAllTestProblems());
    }

    /**
     * @return an array of all the test problems to show in a droplist selector.
     */
    private static OptimizeeProblem[] getAllTestProblems() {
        List<OptimizeeProblem> testProblems = new LinkedList<>();

        for (ParabolaMinVariation v : ParabolaMinVariation.values()) {
            testProblems.add(new ParabolaMinFunctionProblem(v));
        }
        for (DominatingSetVariation v : DominatingSetVariation.values()) {
            testProblems.add(new DominatingSetProblem(v));
        }
        testProblems.add(new SevenElevenProblem());
        for (SubsetSumVariation v : SubsetSumVariation.values()) {
            testProblems.add(new SubsetSumProblem(v));
        }
        for (TravelingSalesmanVariation v : TravelingSalesmanVariation.values()) {
            testProblems.add(new TravelingSalesmanProblem(v));
        }
        return testProblems.toArray(new OptimizeeProblem[0]);
    }
}
