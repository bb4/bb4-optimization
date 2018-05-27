// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.types;

import com.barrybecker4.common.format.FormatUtil;
import com.barrybecker4.optimization.parameter1.redistribution.RedistributionFunction;

import scala.util.Random;

/**
 *  represents a general parameter to an algorithm
 *
 *  @author Barry Becker
 */
public abstract class AbstractParameter implements Parameter {

    protected double value = 0.0;
    protected double minValue = 0.0;
    private double maxValue = 0.0;
    private double range = 0.0;
    private String name = null;
    private boolean integerOnly = false;

    protected RedistributionFunction redistributionFunction;

    /**
     * Constructor
     * @param theVal the initial or assign parameter value
     * @param minVal the minimum value that this parameter is allowed to take on
     * @param maxVal the maximum value that this parameter is allowed to take on
     * @param paramName of the parameter
     */
    public AbstractParameter( double theVal, double minVal, double maxVal, String paramName ) {
        value = theVal;
        minValue = minVal;
        maxValue = maxVal;
        range = maxVal - minVal;
        name = paramName;
        integerOnly = false;
    }

    public AbstractParameter( double val, double minVal, double maxVal,
                              String paramName, boolean intOnly ) {
        this(val, minVal, maxVal, paramName);
        integerOnly = intOnly;
    }

    public boolean isIntegerOnly() {
        return integerOnly;
    }

    /**
     * Teak the value of this parameter a little. If r is big, you may be tweaking it a lot.
     *
     * @param r  the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
     *     r is relative to each parameter range (in other words scaled by it).
     */
    public void tweakValue(double r, Random rand) {
        assert Math.abs(r) <= 1.5;
        if (r == 0 ) {
            return;  // no change in the param.
        }

        double change = rand.nextGaussian() * r * getRange();
        value += change;
        if (value > getMaxValue()) {
            value = getMaxValue();
        }
        else if (value < getMinValue()) {
            value = getMinValue();
        }
        setValue(value);
   }

    public void randomizeValue(Random rand) {
        setValue(getMinValue() + rand.nextDouble() * getRange());
    }

    @Override
    public String toString() {

        StringBuilder sa = new StringBuilder( getName() );
        sa.append( " = " );
        sa.append( FormatUtil.formatNumber(getValue()) );
        sa.append( " [" );
        sa.append( FormatUtil.formatNumber(getMinValue()) );
        sa.append( ", " );
        sa.append( FormatUtil.formatNumber(getMaxValue()) );
        sa.append( ']' );
        if (redistributionFunction != null) {
            sa.append(" redistributionFunction=").append(redistributionFunction);
        }
        return sa.toString();
    }

    public Class getType() {
        if (isIntegerOnly()) {
            return int.class; // Integer.TYPE;  //int.class;
        }
        else {
            return float.class; // Float.TYPE; //  float.class;
        }
    }

    public void setValue(double value) {
        validateRange(value);
        this.value = value;
        // if there is a redistribution function, we need to apply its inverse.
        if (redistributionFunction != null) {
            double v = (value - minValue) / getRange();
            this.value =
                    minValue + getRange() * redistributionFunction.getInverseFunctionValue(v);
        }
    }

    public double getValue() {
        double value = this.value;
        if (redistributionFunction != null) {
            double v = (this.value - minValue) / getRange();
            v = redistributionFunction.getValue(v);
            value = v * getRange() + minValue;
        }
        validateRange(value);
        return value;
    }


    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public double getRange() {
        return range;
    }

    public String getName() {
        return name;
    }

    public void setRedistributionFunction(RedistributionFunction function) {
        redistributionFunction = function;
    }

    private void validateRange(double value) {
        assert (value >= minValue && value <= maxValue) :
            "Value " + value + " outside range [" + minValue +", " + maxValue + "] for parameter " + getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractParameter that = (AbstractParameter) o;

        if (integerOnly != that.integerOnly) return false;
        if (Double.compare(that.maxValue, maxValue) != 0) return false;
        if (Double.compare(that.minValue, minValue) != 0) return false;
        if (!that.getNaturalValue().equals(getNaturalValue())) return false;
        //if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = getNaturalValue().hashCode();
        result = (int) (temp ^ (temp >>> 32));
        temp = minValue != +0.0d ? Double.doubleToLongBits(minValue) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = maxValue != +0.0d ? Double.doubleToLongBits(maxValue) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (integerOnly ? 1 : 0);
        return result;
    }

}
