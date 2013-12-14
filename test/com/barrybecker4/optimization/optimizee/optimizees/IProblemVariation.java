package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

/**
 * A variation on an {@code OptimizeeProblem}
 *
 * @author Barry Becker
 */
public interface IProblemVariation {

    /**
     * @return An optimal ordering of the cities to visit such that cost is minimized.
     */
    ParameterArray getExactSolution();


    /**
     * @param opt the strategy type to get the expected error tolerance for.
     * @return the error tolerance percent for a specific optimization strategy
     */
    double getErrorTolerancePercent(OptimizationStrategyType opt);
}
