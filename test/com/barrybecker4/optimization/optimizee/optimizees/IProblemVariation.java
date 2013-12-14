package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.parameter.ParameterArray;

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
}
