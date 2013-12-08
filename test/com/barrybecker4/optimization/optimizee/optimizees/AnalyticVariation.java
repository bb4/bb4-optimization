/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.optimizee.optimizees;

import com.barrybecker4.optimization.parameter.NumericParameterArrayTest;
import com.barrybecker4.optimization.parameter.ParameterArray;
import com.barrybecker4.optimization.strategy.OptimizationStrategyType;

import static com.barrybecker4.optimization.optimizee.optimizees.AnalyticFunctionConsts.*;

/**
 * An enum for different sorts of analytic functions that we might want to test.
 * Different types of 3d planar functions that all have the same maximum.
 *
 * @author Barry Becker
 */
public enum AnalyticVariation implements IProblemVariation {

    PARABOLA {
        /**
         * Smooth inverted parabola with max at 1000.0.
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return  1000 + ((1.0 - Math.pow(P1 - a.get(0).getValue(), 2)
                                 - Math.pow(P2 - a.get(1).getValue(), 2)));
        }

        @Override
        public double getErrorTolerancePercent(OptimizationStrategyType opt) {
            return getErrorTolerancePercent(opt, new double[] {
                    GLOB_SAMP_TOL, RELAXED_TOL, BASE_TOLERANCE,
                    GLOB_SAMP_TOL,  0,  3 * GLOB_SAMP_TOL,  3 * GLOB_SAMP_TOL, BASE_TOLERANCE
            });
        }
    },
    SINUSOIDAL {
        /**
         * This version introduces a bit of sinusoidal noise.
         * @param a the position on the parabolic surface given the specified values of p1 and p2
         * @return fitness value
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return PARABOLA.evaluateFitness(a)
                    + 0.5 * Math.cos((a.get(0).getValue() - P1) * (a.get(1).getValue() - P2)) - 0.5;
        }

        @Override
        public double getErrorTolerancePercent(OptimizationStrategyType opt) {
            return getErrorTolerancePercent(opt, new double[] {
                    GLOB_SAMP_TOL, RELAXED_TOL, 0.01, GLOB_SAMP_TOL,  RELAXED_TOL, 0.072, 0.072, BASE_TOLERANCE
            });
        }
    },
    ABS_SINUSOIDAL {
        /**
         * This version introduces a bit of absolute value sinusoidal noise.
         * This means it will not be second order differentiable, making this type of search harder.
         * @param a the position on the parabolic surface given the specified values of p1 and p2
         * @return fitness value
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return PARABOLA.evaluateFitness(a)
                    + 0.5 * Math.abs(Math.cos((a.get(0).getValue() - P1) * (a.get(1).getValue() - P2))) - 0.5;
        }

        @Override
        public double getErrorTolerancePercent(OptimizationStrategyType opt) {
            return getErrorTolerancePercent(opt, new double[] {
                    GLOB_SAMP_TOL, 0.0128, 0.01, 2*GLOB_SAMP_TOL,  RELAXED_TOL, 2*GLOB_SAMP_TOL,  3*GLOB_SAMP_TOL,  BASE_TOLERANCE
            });
        }
    },
    STEPPED  {
        /**
         *  This version introduces a bit of step function noise.
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return PARABOLA.evaluateFitness(a) - 0.2 * Math.round( Math.abs((P1
                    - a.get(0).getValue())) * Math.abs((P2 - a.get(1).getValue())));
        }

        @Override
        public double getErrorTolerancePercent(OptimizationStrategyType opt) {
            return getErrorTolerancePercent(opt, new double[] {
                    GLOB_SAMP_TOL, RELAXED_TOL, BASE_TOLERANCE, GLOB_SAMP_TOL, RELAXED_TOL,  0.06,  0.06, BASE_TOLERANCE
            });
        }
    };


    /**
     * Evaluate fitness for the analytics function. Higher values are more fit.
     * @param a the position on the parabolic surface given the specified values of p1 and p2
     * @return fitness value
     */
    public abstract double evaluateFitness(ParameterArray a);

    /**
     * Error tolerance for each search strategy and variation of the problem.
     * @param opt optimization strategy.
     * @return error tolerance percent
     */
    public abstract double getErrorTolerancePercent(OptimizationStrategyType opt);

    public ParameterArray getExactSolution() {
        return AnalyticFunctionConsts.EXACT_SOLUTION;
    }

    protected double getErrorTolerancePercent(OptimizationStrategyType opt, double[] percentValues) {

        double percent = 0;
        switch (opt) {
            case GLOBAL_SAMPLING : percent = percentValues[0]; break;
            case GLOBAL_HILL_CLIMBING : percent = percentValues[1]; break;
            case HILL_CLIMBING : percent = percentValues[2]; break;
            case SIMULATED_ANNEALING : percent = percentValues[3]; break;
            case TABU_SEARCH: percent = percentValues[4]; break;
            case GENETIC_SEARCH : percent = percentValues[5]; break;
            case CONCURRENT_GENETIC_SEARCH : percent = percentValues[6]; break;
            case STATE_SPACE: percent = percentValues[7]; break;
        }
        return percent;
    }

    public static void main(String[] args) {
        double p1 = 0.97795;
        double p2 = 1.975;
        System.out.println("f("+p1+", "+ p2 +")="
                + PARABOLA.evaluateFitness(NumericParameterArrayTest.createParamArray(p1, p2)));

        //Which evaluates to: 1001.4977043582815
    }


}