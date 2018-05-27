// Copyright by Barry G. Becker, 2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.types;

/**
 * @author Barry Becker
 */
public class DoubleParameterTest extends ParameterTst {


    @Override
    protected Parameter createParameter() {
        return new DoubleParameter(2.0, 0.0, 5.0, "double param");
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
        return 2.0;
    }

    @Override
    protected Double expectedNaturalValue() {
        return 2.0;
    }

    @Override
    protected double expectedForwardEpsChange() {
        return 2.16666666667;
    }

    @Override
    protected double expectedBackwardEpsChange() {
        return 1.8333333333333333;
    }

    @Override
    protected Double[] expectedTweakedValues() {
        return new Double[] {2.063847575097573, 1.9887453440192462, 0.042689875177752246, 0.0, 0.09535999327768793};
    }

}
