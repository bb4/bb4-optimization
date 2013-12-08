/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.optimizee.Optimizee;
import com.barrybecker4.optimization.parameter.improvement.Improvement;
import com.barrybecker4.optimization.parameter.types.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *  represents a 1 dimensional array of unique permuted parameters.
 *  There are no duplicates among the parameters, and this array holds them in some permuted order.
 *  This sort of parameter array could be used to represent the order of cities visited in
 *  the traveling salesman problem, for example.
 *
 *  @author Barry Becker
 */
public class PermutedParameterArray extends AbstractParameterArray {

    /** Default constructor */
    protected PermutedParameterArray() {}

    /**
     * Constructor
     * @param params an array of params to initialize with.
     */
    public PermutedParameterArray(Parameter[] params) {
        super(params);
    }

    /**
     * Constructor
     * @param params an list of params to initialize with.
     */
    public PermutedParameterArray(List<Parameter> params) {
        super(params);
    }

    @Override
    protected PermutedParameterArray createInstance() {
        return new PermutedParameterArray();
    }

    protected ParameterArray reverse() {
        AbstractParameterArray paramCopy = this.copy();
        int len = size();

        for (int i=0; i<len/2; i++) {
            Parameter temp = paramCopy.params_.get(i);
            paramCopy.params_.set(i, paramCopy.params_.get(len - i - 1));
            paramCopy.params_.set(len - i - 1, temp);
        }
        return paramCopy;
    }

    /**
     * The distance computation will be quite different for this than a regular parameter array.
     * We want the distance to represent a measure of the amount of similarity between two permutations.
     * If there are similar runs between two permutations, then the distance should be relatively small.
     * N^2 operation, where N is the number of params.
     * @return the distance between this parameter array and another.
     */
    public double distance( ParameterArray pa )  {
        assert ( size() == pa.size() );

        ParameterArray paReverse = ((PermutedParameterArray) pa).reverse();
        return Math.min(difference(pa), difference(paReverse));
    }

    /**
     * The amount of difference can be used as a measure of distance
     * @param pa parameter array
     * @return the amount of difference between pa and ourselves.
     */
    public double difference(ParameterArray pa)  {

        List<Integer> runLengths = new LinkedList<>();
        int len = size();
        int i = 0;

        while (i < len) {
            int runLength = determineRunLength(pa, len, i, runLengths);
            i += runLength;
        }
        return calcDistance(runLengths);
    }

    /**
     * Adds the computed runlength to the runLengths list.
     * @return the computed runlength
     */
    private int determineRunLength(ParameterArray pa, int len, int i, List<Integer> runLengths) {
        int k;
        int ii = i;
        k = 1;
        int j = findCorrespondingEntryIndex(pa, len, get(i));

        boolean matchFound = false;
        boolean matched;
        do {
            ii = ++ii % len;
            j = ++j % len;
            k++;
            matched = this.get(ii).equals(pa.get(j));
            matchFound |= matched;
        } while (matched && k<=len);

        int runLength = k-1;

        if (matchFound) {
            runLengths.add(runLength);
        }
        return runLength;
    }

    /**
     * @return  the entry in pa that corresponds to param.
     * @throws AssertionError if not there. It must be there.
     */
    private int findCorrespondingEntryIndex(ParameterArray pa, int len, Parameter param) {
        int j=0;
        while (j<len && !param.equals(pa.get(j)) ) {
            j++;
        }
        assert (j<len) : "Param "+  param +  " did not match any values in "+ pa;
        return j;
    }

    /**
     * Find the distance between two permutations that each have runs of the specified lengths.
     * @param runLengths list of run lengths.
     * @return the approximate distance between two permutations.
     */
    private double calcDistance(List<Integer> runLengths) {

        // careful this could overflow if the run is really long.
        // If it does we may need to switch to BigInteger.
        double max = Math.pow(2, size());

        if (runLengths.isEmpty()) return max;

        double denom = 0;
        for (int run : runLengths) {
           denom += Math.pow(2, run-1);
        }
        return max / denom - 2.0;
    }

    /**
     * Create a new permutation that is not too distant from what we have now.
     * @param radius a indication of the amount of variation to use. 0 is none, 3 is a lot.
     *   Change Math.min(1, 10 * radius * N/100) of the entries, where N is the number of params
     * @return the random nbr.
     */
    public PermutedParameterArray getRandomNeighbor(double radius) {

        if (size() <= 1) return this;

        int numToSwap = Math.max(1, (int)(10.0 * radius * size() / 100.0));

        PermutedParameterArray nbr = (PermutedParameterArray)this.copy();
        for ( int k = 0; k < numToSwap; k++ ) {
            int index1 = MathUtil.RANDOM.nextInt(size());
            int index2 = MathUtil.RANDOM.nextInt(size());
            while (index2 == index1) {
                index2 = MathUtil.RANDOM.nextInt(size());
            }
            Parameter temp =  nbr.params_.get(index1);
            nbr.params_.set(index1, nbr.params_.get(index2));
            nbr.params_.set(index2, temp);
        }
        return nbr;
    }

    /**
     * Globally sample the parameter space.
     * @param requestedNumSamples approximate number of samples to retrieve.
     *   If the problem space is small and requestedNumSamples is large, it may not be possible to return this
     *   many unique samples.
     * @return some number of unique samples.
     */
    public List<ParameterArray> findGlobalSamples(int requestedNumSamples) {

        // Divide by 2 because it does not matter which param we start with.
        // See page 13 in How to Solve It.
        long numPermutations = MathUtil.factorial(size()) / 2;

        // if the requested number of samples is close to the total number of permutations,
        // then we could just enumerate the permutations.
        double closeFactor = 0.7;
        int numSamples = requestedNumSamples;

        if (requestedNumSamples > closeFactor *numPermutations) {
            numSamples = (int)(closeFactor * numPermutations);
        }

        List<ParameterArray> globalSamples = new ArrayList<>(numSamples);

        while (globalSamples.size() < numSamples) {

            ParameterArray nextSample = this.getRandomSample();
            if (!globalSamples.contains(nextSample)) {
                globalSamples.add(nextSample);
            }
        }
        return globalSamples;
    }

    /**
     * {@inheritDoc}
     * Try swapping parameters randomly until we find an improvement (if we can);
     */
    public Improvement findIncrementalImprovement(Optimizee optimizee, double jumpSize,
                                                  Improvement lastImprovement, Set<ParameterArray> cache) {
        int maxTries = 1000;
        int numTries = 0;
        double fitnessDelta;
        Improvement improvement = new Improvement(this, 0, jumpSize);

        do {
            PermutedParameterArray nbr = getRandomNeighbor(jumpSize);
            fitnessDelta = 0;

            if (!cache.contains(nbr)) {
                cache.add(nbr);
                if (optimizee.evaluateByComparison()) {
                    fitnessDelta = optimizee.compareFitness(nbr, this);
                } else {
                    double fitness = optimizee.evaluateFitness(nbr);
                    fitnessDelta = getFitness() - fitness;
                    nbr.setFitness(fitness);
                }

                if (fitnessDelta > 0) {
                    improvement = new Improvement(nbr, fitnessDelta, jumpSize);
                }
            }
            numTries++;

        }  while (fitnessDelta <= 0 && numTries < maxTries);

        return improvement;
    }

    /**
     * @return get a completely random solution in the parameter space.
     */
    public ParameterArray getRandomSample() {

        List<Parameter> theParams = new ArrayList<>(params_);
        Collections.shuffle(theParams, MathUtil.RANDOM);

        List<Parameter> newParams = new ArrayList<>(size());
        for (Parameter p : theParams) {
            newParams.add(p.copy());
        }

        return new PermutedParameterArray(newParams);
    }

}
