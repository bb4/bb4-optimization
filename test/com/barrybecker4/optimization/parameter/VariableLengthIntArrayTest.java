// Copyright by Barry G. Becker, 2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.parameter.distancecalculators.DistanceCalculator;
import com.barrybecker4.optimization.parameter.distancecalculators.MagnitudeDistanceCalculator;
import com.barrybecker4.optimization.parameter.distancecalculators.MagnitudeIgnoredDistanceCalculator;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Barry Becker
 */
public class VariableLengthIntArrayTest  {

    private static final double TOL = 0.0;

    /** instance under test */
    private VariableLengthIntArray params;

    @Before
    public void setUp() {
        MathUtil.RANDOM.setSeed(0);
    }

    @Test
    public void testGetNumSteps() {

        params = createDistArray(2, -1, 3, -1);

        assertEquals("Unexpected numSteps", 4, params.getMaxLength());
    }

    @Test
    public void testSimilarityWhenEqual() {

        params = createDistArray(2, -1, 3, -1);
        VariableLengthIntArray otherParams = createDistArray(2, -1, 3, -1);
        assertEquals("Unexpected difference", 0.0, params.distance(otherParams), TOL);
    }

    @Test
    public void testSimilarityWhenEqualSizeButDifferentValues() {

        params = createDistArray(2, -1, 3, -1);
        VariableLengthIntArray otherParams = createDistArray(2, -1, 3, -2);
        assertEquals("Unexpected difference", 1.0, params.distance(otherParams), TOL);
    }

    @Test
    public void testSimilarityWhenEqualSizeButVeryDifferentValues() {

        params = createDistArray(2, -99, 3, -1);
        VariableLengthIntArray otherParams = createDistArray(2, -1, 30, -2);
        assertEquals("Unexpected difference", 124.0, params.distance(otherParams), TOL);
    }

    @Test
    public void testSimilarityWhenUnequalSizes() {

        params = createDistArray(2, -1, 3, -1);
        VariableLengthIntArray otherParams = createDistArray(2, -1, 3, -1, 1);
        assertEquals("Unexpected difference", 1.0, params.distance(otherParams), TOL);
    }

    @Test
    public void testGetSamplePopulationSizeWhenSmall() {

        params = createDistArray(2, -1, 3, -1);
        assertEquals("Unexpected numSteps", 16, params.getSamplePopulationSize());
    }

    @Test
    public void testGetSamplePopulationSizeWhenLarge() {

        params = createDistArray(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6, -17, 11);
        assertEquals("Unexpected numSteps", 4000, params.getSamplePopulationSize());
    }

    @Test
    public void testFind0GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);
        params.findGlobalSamples(0);
    }

    @Test
    public void testFind1GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);

        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(1));
        assertEquals("Unexpected num samples", 1, samples.size());

        List<VariableLengthIntArray> expParams = Arrays.asList(createDistArray(2, 3, -1));
        assertEquals("Unexpected params", expParams, samples);
    }

    @Test
    public void testFind2GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);
        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(2));
        assertEquals("Unexpected num samples", 2, samples.size());
    }

    @Test
    public void testFind3GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);
        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(3));
        assertEquals("Unexpected num samples", 3, samples.size());
    }

    @Test
    public void testFind4GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);

        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(4));
        assertEquals("Unexpected num samples", 4, samples.size());

        List<VariableLengthIntArray> expParams = Arrays.asList(
                createDistArray(2, 3, -1),
                createDistArray(2, -1),
                createDistArray(3, -1),
                createDistArray(-1, -1));
        assertEquals("Unexpected params", expParams, samples);
    }

    @Test
    public void testFind10GlobalSamples() {

        params = createDistArray(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6);
        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(10));
        assertEquals("Unexpected num samples", 10, samples.size());
    }

    @Test
    public void testFind97GlobalSamples() {

        params = createDistArray(2, -1, 3, -1);

        List<VariableLengthIntArray> samples = getListFromIterator(params.findGlobalSamples(97));
        assertEquals("Unexpected num samples", 15, samples.size());
    }


    private List<VariableLengthIntArray> getListFromIterator(Iterator<VariableLengthIntArray> iter) {
        List<VariableLengthIntArray> list = new ArrayList<>();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public VariableLengthIntArray createArray(DistanceCalculator dCalc, Integer... numberList) {
        int numNodes = numberList.length;
        List<Parameter> params = new ArrayList<>(numNodes);
        for (int i : numberList) {
            params.add(createParam(i));
        }
        return new VariableLengthIntArray(params, Arrays.asList(numberList), dCalc);
    }

    public VariableLengthIntArray createDistArray(Integer... numberList) {
        return createArray(new MagnitudeDistanceCalculator(), numberList);
    }

    public VariableLengthIntArray createDistIgnoredArray(Integer... numberList) {
        return createArray(new MagnitudeIgnoredDistanceCalculator(), numberList);
    }

    private Parameter createParam(int i) {
        int min = i < 0 ? i : 0;
        int max = i >= 0 ? i : 0;
        return new IntegerParameter(i, min, max, "p" + i);
    }

}
