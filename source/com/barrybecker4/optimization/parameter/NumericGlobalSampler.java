/** Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MultiArray;
import com.barrybecker4.optimization.parameter.types.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds a set of uniformly distributed global samples in a large numeric parameter space.
 *
 * @author Barry Becker
 */
public class NumericGlobalSampler {

    private NumericParameterArray params;

    /**
     *  Constructor
     * @param params an array of params to initialize with.
     */
    public NumericGlobalSampler(NumericParameterArray params) {
        this.params = params;
    }

    /**
     * Globally sample the parameter space with a uniform distribution.
     * @@ it would be better to produce an iterator instead of a list so that
     * the memory for the whole list (which could be enormous) would not have to allocated at the start.
     *
     * @param requestedNumSamples approximate number of samples to retrieve.
     *   If the problem space is small and requestedNumSamples is large, it may not be possible to return this
     *   many unique samples.
     * @return some number of unique samples.
     */
    public List<NumericParameterArray> findGlobalSamples(int requestedNumSamples) {
        int numDims = params.size();
        int i;
        int[] dims = new int[numDims];

        int samplingRate = (int)Math.pow((double)requestedNumSamples, 1.0/numDims);
        int numSamples = determineNumSamples(dims, samplingRate);

        MultiArray samples = new MultiArray( dims );
        List<NumericParameterArray> globalSamples = new ArrayList<>(numSamples);

        for ( i = 0; i < samples.getNumValues(); i++ ) {
            int[] index = samples.getIndexFromRaw( i );
            NumericParameterArray nextSample = params.copy();

            for ( int j = 0; j < nextSample.size(); j++ ) {
                Parameter p = nextSample.get( j );
                double increment = (p.getMaxValue() - p.getMinValue()) / samplingRate;
                p.setValue(p.getMinValue() + increment / 2.0 + index[j] * increment);
            }

            globalSamples.add(nextSample);
        }
        return globalSamples;
    }

    /**
     * @param dims array of dimensions
     * @param samplingRate sampling rate along each numeric dimension.
     * @return the number of global samples to find
     */
    private int determineNumSamples(int[] dims, int samplingRate) {
        int i;
        int numSamples = 1;

        for ( i = 0; i < dims.length; i++ ) {
            dims[i] = samplingRate;
            numSamples *= dims[i];
        }
        return numSamples;
    }
}
