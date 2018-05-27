// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.types;

/**
 * @author Barry Becker
 */
public class IntegerParameterTest extends ParameterTst {


    @Override
    protected Parameter createParameter() {
        return new IntegerParameter(4, 3, 11, "integer param");
    }

    @Override
    protected boolean expectedIsIntegerOnly() {
       return true;
    }

    @Override
    protected double expectedMinValue() {
        return 3.0;
    }
    @Override
    protected double expectedMaxValue() {
        return 11.0;
    }

    @Override
    protected double expectedRange() {
        return 8.0;
    }

    @Override
    protected double expectedValue() {
        return 4.0;
    }

    @Override
    protected Long expectedNaturalValue() {
        return 4L;
    }

    @Override
    protected double expectedForwardEpsChange() {
        return 5.0;
    }

    @Override
    protected double expectedBackwardEpsChange() {
        return 3.0;
    }

    @Override
    protected Integer[] expectedTweakedValues() {
        return new Integer[] {4, 5, 7, 3, 3};
    }
}
