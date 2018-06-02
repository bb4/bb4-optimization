// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy1;

import com.barrybecker4.optimization.Logger1;
import com.barrybecker4.optimization.OptimizationListener1;
import com.barrybecker4.optimization.optimizee1.Optimizee;
import com.barrybecker4.optimization.Optimizer1;
import com.barrybecker4.optimization.parameter1.ParameterArray;

/**
 * Abstract base class for Optimization strategy.
 *
 * This and derived classes uses the strategy design pattern.
 * @see Optimizer1
 * @see Optimizee
 *
 * @author Barry Becker
 */
public abstract class OptimizationStrategy {

    /** The thing to be optimized */
    Optimizee optimizee;

    private Logger1 logger;

    /** listen for optimization changed events. useful for debugging.  */
    protected OptimizationListener1 listener;

    /**
     * Constructor
     * @param optimizee the thing to be optimized.
     */
    public OptimizationStrategy( Optimizee optimizee ) {
        this.optimizee = optimizee;
    }

    /**
     * @param logger the file that will record the results
     */
    public void setLogger(Logger1 logger) {
        this.logger = logger;
    }

    protected void log(int iteration, double fitness, double jumpSize, double deltaFitness,
                      ParameterArray params, String msg) {
        if (logger != null)
            logger.write(iteration, fitness, jumpSize, deltaFitness, params, msg);
    }

    /**
     * @param initialParams the initial guess at the solution.
     * @param fitnessRange the approximate absolute value of the fitnessRange.
     * @return optimized parameters.
     */
    public abstract ParameterArray doOptimization(ParameterArray initialParams, double fitnessRange);

    public void setListener(OptimizationListener1 listener) {
        this.listener = listener;
    }

    /**
     * @param currentBest current best parameter set.
     * @return true if the optimal fitness has been reached.
     */
    boolean isOptimalFitnessReached(ParameterArray currentBest) {
        boolean optimalFitnessReached = false;

        if (!optimizee.evaluateByComparison()) {
            assert optimizee.getOptimalFitness() >= 0;
            optimalFitnessReached = currentBest.getFitness() <= optimizee.getOptimalFitness();
        }
        return optimalFitnessReached;
    }

    void notifyOfChange(ParameterArray params) {
        if (listener != null) {
            listener.optimizerChanged(params);
        }
    }
}
