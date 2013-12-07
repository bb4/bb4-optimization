/** Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.optimizee.Optimizee;
import com.barrybecker4.optimization.parameter.improvement.Improvement;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *  represents a 1 dimensional, variable length, array of unique integer parameters.
 *  The order of the integers does not matter.
 *
 *  @author Barry Becker
 */
public class VariableLengthIntegerParameterArray extends AbstractParameterArray {

    /** the maximum number of params in the array that is possible */
    private int maxLength;


    /** Default constructor */
    protected VariableLengthIntegerParameterArray() {}

    /**
     * Constructor
     * @param params an array of params to initialize with.
     */
    public VariableLengthIntegerParameterArray(List<Parameter> params, int max) {
        super(params);
        maxLength = max;
    }

    @Override
    protected VariableLengthIntegerParameterArray createInstance() {
        return new VariableLengthIntegerParameterArray();
    }

    /**
     * The distance computation will be quite different for this than a regular parameter array.
     * We want the distance to represent a measure of the amount of similarity between two instances.
     * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
     *  - the length of the parameter array
     *  - the set of values in the parameter array.
     *  Generally, the distance is greater if the number of params is different.
     * @return the distance between this parameter array and another.
     */
    public double distance( ParameterArray pa )  {
        int thisLength = params_.length;
        int thatLength = pa.size();

        List<Integer> theseValues = new ArrayList<>(thisLength);
        List<Integer> thoseValues = new ArrayList<>(thatLength);

        for (Parameter p : params_) {
            theseValues.add((int)p.getValue());
        }
        for (int i=0; i< thatLength; i++) {
            thoseValues.add((int)pa.get(i).getValue());
        }

        Collections.sort(theseValues);
        Collections.sort(thoseValues);

        int valueDifferences = calcValueDifferences(theseValues, thoseValues);

        return Math.abs(thisLength - thatLength) + valueDifferences;
    }

    /**
     * Perform a sort of merge sort on the two sorted lists of values to find matches.
     * The more matches there are between the two lists, the more similar they are.
     * The magnitude of the differences between values does not matter, only whether
     * they are the same or different.
     * @param theseValues first ordered list
     * @param thoseValues second ordered list
     * @return measure of the difference between the two sorted lists.
     *   It will return 0 if the two lists are the same.
     */
    private int calcValueDifferences(List<Integer> theseValues, List<Integer> thoseValues) {

        int thisLen = theseValues.size();
        int thatLen = thoseValues.size();
        int thisCounter = 0;
        int thatCounter = 0;
        int matchCount = 0;

        while (thisCounter < thisLen && thatCounter < thatLen) {
            double thisVal = theseValues.get(thisCounter);
            double thatVal = thoseValues.get(thatCounter);
            if (thisVal < thatVal) {
                thisCounter++;
            }
            else if (thatVal > thisVal) {
                thatCounter++;
            }
            else {  // they are the same
                thisCounter++;
                thatCounter++;
                matchCount++;
            }
        }
        return Math.max(thisLen, thatLen) - matchCount;
    }

    /**
     * Create a new permutation that is not too distant from what we have now.
     * The two ways a configuration of marked nodes can change is
     *  - add or remove nodes
     *  - change values of nodes
     * @param radius a indication of the amount of variation to use. 0 is none, 2 is a lot.
     *   Change Math.min(1, 10 * radius * N/100) of the entries, where N is the number of params
     * @return the random nbr.
     */
    public VariableLengthIntegerParameterArray getRandomNeighbor(double radius) {

        if (size() <= 1) return this;

        double probAddRemove = 1.0 -  2.0/(2.0 + radius);
        boolean add = false;
        boolean remove = false;
        if (MathUtil.RANDOM.nextDouble() > probAddRemove) {
            if ((MathUtil.RANDOM.nextDouble() > 0.5 || size() <= 1) && size() < maxLength-1 ) {
                add = true;
            }
            else {
                remove = true;
            }
        }
        int numNodesToMove = 0;
        VariableLengthIntegerParameterArray nbr = (VariableLengthIntegerParameterArray)this.copy();

        if (add || remove) {
            numNodesToMove = MathUtil.RANDOM.nextInt(Math.min(size(), (int)(radius + 1)));
        }
        else {
            numNodesToMove = 1 + MathUtil.RANDOM.nextInt(1 + (int)radius);
        }
        //System.out.println("adding = "+ add + " removing = "+ remove + " moving = "+ numNodesToMove);

        if (remove) {
            removeRandomParam(nbr);
        }
        if (add) {
            addRandomParam(nbr);
        }
        moveNodes(numNodesToMove, nbr);

        return nbr;
    }

    private void removeRandomParam(VariableLengthIntegerParameterArray nbr) {
        int indexToRemove = MathUtil.RANDOM.nextInt(size());
        assert nbr.size() > 0;
        Parameter[] newParams = new Parameter[nbr.size()-1];
        int ct = 0;
        for (int i=0; i < nbr.size(); i++) {
            if (i != indexToRemove) {
                newParams[ct++] = nbr.get(i);
            }
        }
        nbr.params_ = newParams;
    }

    private void addRandomParam(VariableLengthIntegerParameterArray nbr) {

        List<Integer> freeNodes = getFreeNodes(nbr);
        int newSize = nbr.size() + 1;
        int ct = 0;
        assert newSize <= maxLength;
        Parameter[] newParams = new Parameter[newSize];
        for (Parameter p : nbr.params_) {
            newParams[ct++] = p;
        }
        int value = freeNodes.get(MathUtil.RANDOM.nextInt(freeNodes.size()));
        newParams[ct] = new IntegerParameter(value, 0, maxLength-1, "p" + value);
        nbr.params_ = newParams;
    }

    /**
     * select num free nodes randomly and and swap them with num randomly selected marked nodes.
     * @param numNodesToMove
     * @param nbr neighbor parameter array
     */
    private void moveNodes(int numNodesToMove, VariableLengthIntegerParameterArray nbr) {
        List<Integer> freeNodes = getFreeNodes(nbr);

        List<Integer> swapNodes = selectRandomNodes(numNodesToMove, freeNodes);

        for (int i=0; i<numNodesToMove; i++) {
            int index = MathUtil.RANDOM.nextInt(nbr.size());
            nbr.get(index).setValue(swapNodes.get(i));
        }
    }

    private List<Integer> selectRandomNodes(int numNodes, List<Integer> freeNodes) {
        List<Integer> selected = new LinkedList<>();
        for (int i=0; i<numNodes; i++) {
            int node = freeNodes.get(MathUtil.RANDOM.nextInt(freeNodes.size()));
            selected.add(node);
            freeNodes.remove((Integer) node);
        }
        return selected;
    }

    private List<Integer> getFreeNodes(VariableLengthIntegerParameterArray nbr) {
        List<Integer> freeNodes = new ArrayList<>(maxLength);
        Set<Integer> markedNodes = new HashSet<>();
        for (Parameter p : nbr.params_) {
            markedNodes.add((int)p.getValue());
        }

        for (int i = 0; i < maxLength; i++) {
            if (!markedNodes.contains(i))   {
                freeNodes.add(i);
            }
        }
        return freeNodes;
    }

    /**
     * Globally sample the parameter space.
     * @param requestedNumSamples approximate number of samples to retrieve.
     * If the problem space is small and requestedNumSamples is large, it may not be possible to return this
     * many unique samples.
     * @return some number of unique samples.
     */
    public List<ParameterArray> findGlobalSamples(int requestedNumSamples) {

        // Divide by 2 because it does not matter which param we start with.
        // See page 13 in How to Solve It.
        long numPermutations = Long.MAX_VALUE;
        if (maxLength <= 20)  {
            numPermutations = MathUtil.factorial(maxLength) / 2;
        }

        // if the requested number of samples is close to the total number of permutations,
        // then we could just enumerate the permutations.
        double closeFactor = 0.7;
        int numSamples = requestedNumSamples;

        if (requestedNumSamples > closeFactor * numPermutations) {
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
            VariableLengthIntegerParameterArray nbr = getRandomNeighbor(jumpSize);
            fitnessDelta = 0;

            if (!cache.contains(nbr)) {
                cache.add(nbr);
                if (optimizee.evaluateByComparison()) {
                    fitnessDelta = optimizee.compareFitness(nbr, this);
                } else {
                    double fitness = optimizee.evaluateFitness(nbr);
                    fitnessDelta = fitness - getFitness();
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

        List<Integer> marked = new LinkedList<>();
        for (int i=0; i<maxLength; i++) {
            if (MathUtil.RANDOM.nextDouble() > 0.5) {
                marked.add(i);
            }
        }
        List<Parameter> newParams = new ArrayList<>();
        for (int markedNode : marked) {
            newParams.add(new IntegerParameter(markedNode, 0, maxLength-1, "p" + markedNode));
        }

        return new VariableLengthIntegerParameterArray(newParams, maxLength);
    }


    /**
     * @return a copy of ourselves.
     */
    public AbstractParameterArray copy() {
        VariableLengthIntegerParameterArray copy = (VariableLengthIntegerParameterArray) super.copy();
        copy.maxLength = maxLength;
        return copy;
    }

}
