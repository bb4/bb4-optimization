package com.barrybecker4.optimization.parameter1.distancecalculators;

import com.barrybecker4.optimization.parameter1.ParameterArray;

/**
 * Calculates the distance between parameter arrays
 * @author Barry Becker
 */
public interface DistanceCalculator {

    /** @return distance between parameter arrays */
    double calculateDistance(ParameterArray pa1, ParameterArray pa2);
}
