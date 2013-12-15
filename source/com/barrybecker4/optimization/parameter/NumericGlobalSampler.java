/** Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MultiArray;
import com.barrybecker4.optimization.parameter.types.Parameter;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Finds a set of uniformly distributed global samples in a large numeric parameter space.
 *
 * @author Barry Becker
 */
public class NumericGlobalSampler implements Iterator<NumericParameterArray> {

    private NumericParameterArray params;

    /** number of discrete samples to take along each parameter */
    private int samplingRate;

    private MultiArray samples;

    /** becomes false when no more samples to iterate through */
    private boolean hasNext = true;

    /** counts up to the number of samples as we iterate */
    private long counter = 0;

    /**
     * approximate number of samples to retrieve.
     * If the problem space is small and requestedNumSamples is large, it may not be possible to return this
     * many unique samples.
     */
    private long numSamples;

    /**
     *  Constructor
     * @param params an array of params to initialize with.
     */
    public NumericGlobalSampler(NumericParameterArray params, long requestedNumSamples) {
        this.params = params;

        int[] dims = new int[params.size()];
        samplingRate = determineSamplingRate(requestedNumSamples);
        for (int i=0; i<dims.length; i++) {
            dims[i] = samplingRate;
        }
        // this potentially takes a lot of memory - may need to revisit
        samples = new MultiArray(dims);

        numSamples = samples.getNumValues();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public NumericParameterArray next() {
        if (counter >= numSamples) {
            throw new NoSuchElementException("ran out of samples.");
        }
        if (counter == numSamples-1) {
            hasNext = false;
        }

        int[] index = samples.getIndexFromRaw((int)counter);     // revisit
        NumericParameterArray nextSample = params.copy();

        for ( int j = 0; j < nextSample.size(); j++ ) {
            Parameter p = nextSample.get( j );
            double increment = (p.getMaxValue() - p.getMinValue()) / samplingRate;
            p.setValue(p.getMinValue() + increment / 2.0 + index[j] * increment);
        }
        counter++;
        return nextSample;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("cannot remove samples from the iterator");
    }

    private int determineSamplingRate(long requestedNumSamples) {
        int numDims = params.size();
        return (int)Math.pow((double)requestedNumSamples, 1.0 / numDims);
    }

}
