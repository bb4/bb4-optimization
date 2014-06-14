// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.parameter.Direction;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;

/**
 * Base class for all parameter test classes
 *
 * @author Barry Becker
 */
public abstract class ParameterTst {

    private static final double TOL = 0.0;

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
                expectedForwardEpsChange(), parameter.getValue(), MathUtil.EPS_MEDIUM);
    }

    @Test
    public void testIncrementByEpsBackward() {
        parameter.incrementByEps(Direction.BACKWARD);
        assertEquals("Unexpected eps backward",
                expectedBackwardEpsChange(), parameter.getValue(), MathUtil.EPS_MEDIUM);
    }

    protected boolean expectedIsIntegerOnly() {
        return false;
    }

    protected abstract double expectedMinValue();
    protected abstract double expectedMaxValue();
    protected abstract double expectedRange();
    protected abstract double expectedValue();
    protected abstract Object expectedNaturalValue();
    protected abstract double expectedForwardEpsChange();
    protected abstract double expectedBackwardEpsChange();
}
