// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.optimizee1.optimizees.OptimizeeProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaMinFunctionProblem;
import com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaMinVariation;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

/**
 * @author Barry Becker
 */
public class TestParabolaMinFunctionProblem1 extends OptimizerTestCase1 {

    @Override
    protected void doTest(OptimizationStrategyType optimizationType) {

        for (ParabolaMinVariation variation : ParabolaMinVariation.values()) {

            MathUtil.RANDOM().setSeed(0);
            OptimizeeProblem problem = new ParabolaMinFunctionProblem(variation);
            verifyProblem(problem, variation, optimizationType);
        }
    }
}
