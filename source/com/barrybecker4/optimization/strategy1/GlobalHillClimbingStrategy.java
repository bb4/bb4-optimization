// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy1;

import com.barrybecker4.optimization.optimizee1.Optimizee;
import com.barrybecker4.optimization.parameter1.ParameterArray;

/**
 * This is a hybrid optimization strategy.
 * @see GlobalSampleStrategy
 * @see HillClimbingStrategy
 *
 * @author Barry Becker
 */
public class GlobalHillClimbingStrategy extends OptimizationStrategy {

    private static final int NUM_SAMPLES = 1000;

    /**
     * Constructor.
     * use a hardcoded static data interface to initialize.
     * so it can be easily run in an applet without using resources.
     * @param optimizee the thing to be optimized.
     */
    public GlobalHillClimbingStrategy( Optimizee optimizee ) {
        super(optimizee);
    }

    /**
     * Perform the optimization of the optimizee.
     * @param params parameter array
     * @param fitnessRange the approximate absolute value of the fitnessRange.
     * @return optimized params
     */
    @Override
    public ParameterArray doOptimization( ParameterArray params, double fitnessRange ) {

        GlobalSampleStrategy gsStrategy = new GlobalSampleStrategy(optimizee);
        gsStrategy.setListener(listener);
        // 3 sample points along each dimension
        gsStrategy.setSamplingRate(NUM_SAMPLES);

        // first find a good place to start
        // perhaps we should try several of the better results from global sampling.
        ParameterArray sampledParams = gsStrategy.doOptimization(params, fitnessRange);

        OptimizationStrategy strategy = new HillClimbingStrategy(optimizee);
        strategy.setListener(listener);
        return strategy.doOptimization(sampledParams, fitnessRange);
    }
}