// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types;

/**
 * @author Barry Becker
 */
public class DoubleParameterTest extends ParameterTst {


    @Override
    protected Parameter createParameter() {
        return new DoubleParameter(1.0, 0.0, 5.0, "double param");
    }

    @Override
    protected double expectedMinValue() {
        return 0.0;
    }
    @Override
    protected double expectedMaxValue() {
        return 5.0;
    }

    @Override
    protected double expectedRange() {
        return 5.0;
    }
    @Override
    protected double expectedValue() {
        return 1.0;
    }

    @Override
    protected Double expectedNaturalValue() {
        return new Double(1.0);
    }

    @Override
    protected double expectedForwardEpsChange() {
        return 1.16666666667;
    }

    @Override
    protected double expectedBackwardEpsChange() {
        return 0.83333333333;
    }


}
