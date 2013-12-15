// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * @author Barry Becker
 */
public class NumericParameterArrayTest extends TestCase {

    private static final double MIN_VALUE = -20.0;
    private static final double MAX_VALUE = 20.0;

    /** instance under test */
    private NumericParameterArray params;

    @Override
    public void setUp() {
        MathUtil.RANDOM.setSeed(0);
    }

    public void testGetNumSteps() {

        params = createParamArray(.2, .3);

        assertEquals("Unexpected numSteps", 10, params.getNumSteps());
    }

    public void testGetSamplePopulationSize() {

        params = createParamArray(.2, .3);

        assertEquals("Unexpected numSteps", 36, params.getSamplePopulationSize());
    }

    public void testFind0GlobalSamples() {

        params = createParamArray(.2, .3);

        try {
            params.findGlobalSamples(0);
            fail();
        } catch (IllegalArgumentException e) {
            // success
        }
    }

    public void testFind1GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(1);
        assertEquals("Unexpected num samples", 1, samples.size());

        List<NumericParameterArray> expParams = Arrays.asList(createParamArray(0, 0.0));
        assertEquals("Unexpected params", expParams, samples);
    }

    public void testFind2GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(2);
        assertEquals("Unexpected num samples", 1, samples.size());
    }

    public void testFind3GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(3);
        assertEquals("Unexpected num samples", 1, samples.size());
    }

    public void testFind4GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(4);
        assertEquals("Unexpected num samples", 4, samples.size());

        List<NumericParameterArray> expParams = Arrays.asList(
                createParamArray(-10.0, -10.0),
                createParamArray(-10.0, 10.0),
                createParamArray(10.0, -10.0),
                createParamArray(10.0, 10.0));
        assertEquals("Unexpected params", expParams, samples);
    }

    public void testFind10GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(10);
        assertEquals("Unexpected num samples", 9, samples.size());
    }

    public void testFind97GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(97);
        assertEquals("Unexpected num samples", 81, samples.size());
    }

    public void testFind1000GlobalSamples() {

        params = createParamArray(.2, .3);

        List<NumericParameterArray> samples = params.findGlobalSamples(1000);
        assertEquals("Unexpected num samples", 961, samples.size());
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
