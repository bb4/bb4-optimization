// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types;

/**
 * @author Barry Becker
 */
public class BooleanParameterTest extends ParameterTst {

    @Override
    protected Parameter createParameter() {
        return new BooleanParameter(true, "boolean param");
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
        return 1.0;
    }

    @Override
    protected double expectedRange() {
        return 1.0;
    }

    @Override
    protected double expectedValue() {
        return 1.0;
    }

    @Override
    protected Boolean expectedNaturalValue() {
        return Boolean.TRUE;
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
