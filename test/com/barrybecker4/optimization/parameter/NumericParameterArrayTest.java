// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
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
public class NumericParameterArrayTest {

    private static final double MIN_VALUE = -20.0;
    private static final double MAX_VALUE = 20.0;

    /** instance under test */
    private NumericParameterArray params;

    @Before
    public void setUp() {
        MathUtil.RANDOM().setSeed(0);
    }

    @Test
    public void testGetNumSteps() {

        params = createParamArray(.2, .3);

        assertEquals("Unexpected numSteps", 10, params.getNumSteps());
    }

    @Test
    public void testGetSamplePopulationSize() {

        params = createParamArray(.2, .3);

        assertEquals("Unexpected numSteps", 36, params.getSamplePopulationSize());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFind0GlobalSamples() {

        params = createParamArray(.2, .3);
        params.findGlobalSamples(0);
    }

    @Test
    public void testFind1GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(1));
        assertEquals("Unexpected num samples", 1, samples.size());

        List<NumericParameterArray> expParams = Arrays.asList(createParamArray(0, 0.0));
        assertEquals("Unexpected params", expParams, samples);
    }

    @Test
    public void testFind2GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(2));
        assertEquals("Unexpected num samples", 1, samples.size());
    }

    @Test
    public void testFind3GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(3));
        assertEquals("Unexpected num samples", 1, samples.size());
    }

    @Test
    public void testFind4GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(4));
        assertEquals("Unexpected num samples", 4, samples.size());

        List<NumericParameterArray> expParams = Arrays.asList(
                createParamArray(-10.0, -10.0),
                createParamArray(-10.0, 10.0),
                createParamArray(10.0, -10.0),
                createParamArray(10.0, 10.0));
        assertEquals("Unexpected params", expParams, samples);
    }

    @Test
    public void testFind10GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(10));
        assertEquals("Unexpected num samples", 9, samples.size());
    }

    @Test
    public void testFind97GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(97));
        assertEquals("Unexpected num samples", 81, samples.size());
    }

    @Test
    public void testFind1000GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = getListFromIterator(params.findGlobalSamples(1000));
        assertEquals("Unexpected num samples", 961, samples.size());
    }


    private List<NumericParameterArray> getListFromIterator(Iterator<NumericParameterArray> iter) {
        List<NumericParameterArray> list = new ArrayList<>();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    public static NumericParameterArray createParamArray(double value1, double value2) {
        assert value1 >= MIN_VALUE && value1 <= MAX_VALUE;
        assert value2 >= MIN_VALUE && value2 <= MAX_VALUE;

        return  new NumericParameterArray(
               new double[] {value1, value2}, // values
               new double[] {MIN_VALUE, MIN_VALUE},       // min
               new double[] {MAX_VALUE, MAX_VALUE},       // max
               new String[] {"A", "B"}        // names
        );
    }

}
