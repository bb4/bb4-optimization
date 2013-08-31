// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.optimization.parameter.improvement.Improvement;
import com.barrybecker4.optimization.optimizee.Optimizee;
import com.barrybecker4.optimization.parameter.types.Parameter;

import java.util.List;
import java.util.Set;

/**
 *  represents an array of parameters
 *
 *  @author Barry Becker
 */
public interface ParameterArray extends Comparable<ParameterArray> {

    /**
     * @return the number of parameters in the array.
     */
    int size();

    /**
     * @param value fitness value to assign to this parameter array.
     */
    void setFitness(double value);

    /**
     * @return the fitness value.
     */
    double getFitness();

    /**
     * @return a copy of ourselves.
     */
    ParameterArray copy();

    /**
     * @return the ith parameter in the array.
     */
    Parameter get(int i);

    /**
     * @return a reasonable size for a sampling of values from the parameter space.
     */
    int getSamplePopulationSize();

    /**
     * Globally sample the parameter space with a uniform distribution.
     * @param requestedNumSamples approximate number of samples to retrieve.
     *   If the problem space is small and requestedNumSamples is large, it may not be possible to return this
     *   many unique samples.
     * @return some number of unique samples.
     */
    List<ParameterArray> findGlobalSamples(int requestedNumSamples);

    /**
     * Try to find a parameterArray that is better than what we have now by evaluating using the optimizee passed in.
     * @param optimizee something that can evaluate parameterArrays.
     * @param jumpSize how far to move in the direction of improvement
     * @param lastImprovement the improvement we had most recently. May be null if none.
     * @param cache set of parameters that have already been tested. This is important for cases where the
     *   parameters are discrete and not continuous.
     * @return the improvement which contains the improved parameter array and possibly a revised jumpSize.
     */
    Improvement findIncrementalImprovement(Optimizee optimizee, double jumpSize,
                                           Improvement lastImprovement, Set<ParameterArray> cache);

    /**
     * @return the distance between this parameter array and another.
     * sqrt(sum of squares)
     */
    double distance( ParameterArray pa );

    /**
     * @param radius the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
     *     (relative to each parameter range).
     * @return the random nbr.
     */
    ParameterArray getRandomNeighbor(double radius);

    /**
     * @return get a completely random solution in the parameter space.
     */
    ParameterArray getRandomSample();

    String toString();

    /**
     * @return the parameters in a string of Comma Separated Values.
     */
    String toCSVString();
}
