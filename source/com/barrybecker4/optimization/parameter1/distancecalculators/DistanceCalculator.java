package com.barrybecker4.optimization.parameter1.distancecalculators;

import com.barrybecker4.optimization.parameter1.ParameterArray;

/**
 * @author Barry Becker
 */
public interface DistanceCalculator {
    double calculateDistance(ParameterArray pa1, ParameterArray pa2);
}
