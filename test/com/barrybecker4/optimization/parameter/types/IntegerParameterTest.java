// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types;

/**
 * @author Barry Becker
 */
public class IntegerParameterTest extends ParameterTst {


    @Override
    protected Parameter createParameter() {
        return new IntegerParameter(1, 0, 6, "integer param");
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
        return 1.0;
    }

    @Override
    protected Long expectedNaturalValue() {
        return new Long(1);
    }

    @Override
    protected double expectedForwardEpsChange() {
        return 2.0;
    }

    @Override
    protected double expectedBackwardEpsChange() {
        return 0.0;
    }
}
