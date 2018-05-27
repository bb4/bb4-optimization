package com.barrybecker4.optimization.parameter1.types;

import java.util.Arrays;

/**
 * @author Barry Becker
 */
public class StringParameterTest extends ParameterTst {

    @Override
    protected Parameter createParameter() {
        return new StringParameter(2,
                Arrays.asList("foo", "bar", "baz", "abc", "bcd", "barry", "becker"),
                "integer param");
    }

    @Override
    protected boolean expectedIsIntegerOnly() {
        return true;
    }

    @Override
    protected double expectedMinValue() {
        return 0.0;
    }
    @Override
    protected double expectedMaxValue() {
        return 6.0;
    }

    @Override
    protected double expectedRange() {
        return 6.0;
    }

    @Override
    protected double expectedValue() {
        return 2.0;
    }

    @Override
    protected String expectedNaturalValue() {
        return "baz";
    }

    @Override
    protected double expectedForwardEpsChange() {
        return 3.0;
    }

    @Override
    protected double expectedBackwardEpsChange() {
        return 1.0;
    }

    @Override
    protected String[] expectedTweakedValues() {
        return new String[] {"baz", "baz", "baz", "bcd", "bar"};
    }
}
