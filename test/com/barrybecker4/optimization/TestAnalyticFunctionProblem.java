/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticFunctionProblem;
import com.barrybecker4.optimization.optimizee.optimizees.problems.AnalyticVariation;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

/**
 * @author Barry Becker
 */
public class TestAnalyticFunctionProblem extends OptimizerTestCase {

    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (AnalyticVariation variation : AnalyticVariation.values()) {

            MathUtil.RANDOM.setSeed(0);
            OptimizeeProblem problem = new AnalyticFunctionProblem(variation);
            verityProblem(problem, variation, optimizationType);
        }
    }
}
