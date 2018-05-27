// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.types;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.parameter1.Direction;
import org.junit.Before;
import org.junit.Test;
import scala.util.Random;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Base class for all parameter test classes
 *
 * @author Barry Becker
 */
public abstract class ParameterTst {

    private static final double TOL = 0.0;
    private static final Random RAND = new Random(1);

    /** instance under test */
    protected Parameter parameter;

    @Before
    public void setUp() {
        parameter = createParameter();
    }

    protected abstract Parameter createParameter();


    public void testIsIntegerOnly() {

        assertEquals("Unexpected value for isIntegerOnly", expectedIsIntegerOnly(), parameter.isIntegerOnly());
    }

    public void tetGetMinValue() {
        assertEquals("Unexpected min", expectedMinValue(), parameter.getMinValue(), TOL);
    }

    @Test
    public void tetGetMaxValue() {
        assertEquals("Unexpected max", expectedMaxValue(), parameter.getMaxValue(), TOL);
    }

    @Test
    public void testGetRange() {
        assertEquals("Unexpected min", expectedRange(), parameter.getRange(), TOL);
    }

    @Test
    public void testValue() {
        assertEquals("Unexpected min", expectedValue(), parameter.getValue(), TOL);
    }

    @Test
    public void testNaturalValue() {
        assertEquals("Unexpected natural value",
                expectedNaturalValue(), parameter.getNaturalValue());
    }

    @Test
    public void testIncrementByEpsForward() {
        parameter.incrementByEps(Direction.FORWARD);
        assertEquals("Unexpected eps forward",
                expectedForwardEpsChange(), parameter.getValue(), MathUtil.EPS_MEDIUM());
    }

    @Test
    public void testIncrementByEpsBackward() {
        parameter.incrementByEps(Direction.BACKWARD);
        assertEquals("Unexpected eps backward",
                expectedBackwardEpsChange(), parameter.getValue(), MathUtil.EPS_MEDIUM());
    }

    @Test
    public void testTweakedValues() {
        parameter.tweakValue(0.02, RAND);
        Object v1 = parameter.getNaturalValue();
        parameter.tweakValue(0.1, RAND);
        Object v2 = parameter.getNaturalValue();
        parameter.tweakValue(0.4, RAND);
        Object v3 = parameter.getNaturalValue();
        parameter.tweakValue(0.8, RAND);
        Object v4 = parameter.getNaturalValue();
        parameter.tweakValue(1.1, RAND);
        Object v5 = parameter.getNaturalValue();

        //assertArrayEquals("Unexpected tweaked values", expectedTweakedValues(), new Object[] {v1, v2, v3});
        assertEquals("Unexpected tweaked values",
                Arrays.toString(expectedTweakedValues()),
                Arrays.toString(new Object[] {v1, v2, v3, v4, v5}));
    }

    protected boolean expectedIsIntegerOnly() {
        return false;
    }


    protected abstract double expectedMinValue();
    protected abstract double expectedMaxValue();
    protected abstract double expectedRange();
    protected abstract double expectedValue();
    protected abstract Object[] expectedTweakedValues();
    protected abstract Object expectedNaturalValue();
    protected abstract double expectedForwardEpsChange();
    protected abstract double expectedBackwardEpsChange();
}
