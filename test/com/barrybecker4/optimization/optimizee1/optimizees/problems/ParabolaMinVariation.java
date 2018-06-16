// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee1.optimizees.problems;

import com.barrybecker4.optimization.optimizee1.optimizees.ErrorTolerances;
import com.barrybecker4.optimization.optimizee1.optimizees.IProblemVariation;
import com.barrybecker4.optimization.parameter1.NumericParameterArrayTest;
import com.barrybecker4.optimization.parameter1.ParameterArray;
import com.barrybecker4.optimization.strategy1.OptimizationStrategyType;

import static com.barrybecker4.optimization.optimizee1.optimizees.problems.ParabolaFunctionConsts.*;


/**
 * An enum for different sorts of analytic functions that we might want to test.
 * Different types of 3d planar functions that all have the same maximum.
 * @author Barry Becker
 */
public enum ParabolaMinVariation implements IProblemVariation {

    PARABOLA {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.009, RELAXED_TOL, BASE_TOLERANCE,
                    GLOB_SAMP_TOL,  0,  BASE_TOLERANCE,  BASE_TOLERANCE, BASE_TOLERANCE);

        /** Smooth parabola with min of 0.0 at P1, P2 */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return Math.pow(P1 - a.get(0).getValue(), 2) + Math.pow(P2 - a.get(1).getValue(), 2);
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },
    SINUSOIDAL {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.009, RELAXED_TOL, 0.01, GLOB_SAMP_TOL,
                        RELAXED_TOL, BASE_TOLERANCE, BASE_TOLERANCE, BASE_TOLERANCE);

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
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },
    ABS_SINUSOIDAL {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.009, 0.0128, 0.01, GLOB_SAMP_TOL,
                        RELAXED_TOL, BASE_TOLERANCE,  BASE_TOLERANCE,  BASE_TOLERANCE);

        /**
         * This version introduces a bit of absolute value sinusoidal noise.
         * This means it will not be second order differentiable, making this type of search harder.
         * @param a the position on the parabolic surface given the specified values of p1 and p2
         * @return fitness value
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return PARABOLA.evaluateFitness(a)
                    - 0.5 * Math.abs(Math.cos((a.get(0).getValue() - P1) * (a.get(1).getValue() - P2))) + 0.5;
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    },
    STEPPED  {
        private final ErrorTolerances ERROR_TOLERANCES =
                new ErrorTolerances(0.009, RELAXED_TOL, BASE_TOLERANCE, GLOB_SAMP_TOL,
                        RELAXED_TOL,  BASE_TOLERANCE,  BASE_TOLERANCE, BASE_TOLERANCE);

        /**
         *  This version introduces a bit of step function noise.
         */
        @Override
        public double evaluateFitness(ParameterArray a) {
            return PARABOLA.evaluateFitness(a) +
                    0.2 * Math.round( Math.abs((P1 - a.get(0).getValue())) * Math.abs((P2 - a.get(1).getValue())));
        }

        @Override
        public ErrorTolerances getErrorTolerances() {
            return ERROR_TOLERANCES;
        }
    };


    /**
     * Evaluate fitness for the analytics function. Lower values are more fit.
     * @param a the position on the parabolic surface given the specified values of p1 and p2
     * @return fitness value
     */
    public abstract double evaluateFitness(ParameterArray a);


    public ParameterArray getExactSolution() {
        return ParabolaFunctionConsts.EXACT_SOLUTION;
    }

    /** @return the error tolerance percent for a specific optimization strategy */
    public double getErrorTolerancePercent(OptimizationStrategyType opt) {
        return getErrorTolerances().getErrorTolerancePercent(opt);
    }

    /**
     * Error tolerance for each search strategy and variation of the problem.
     * @return error tolerance percent
     */
    protected abstract ErrorTolerances getErrorTolerances();


    public static void main(String[] args) {
        double p1 = 0.97795;
        double p2 = 1.975;
        System.out.println("f("+p1+", "+ p2 +")="
                + PARABOLA.evaluateFitness(NumericParameterArrayTest.createParamArray(p1, p2)));
        for (ParabolaMinVariation variation : ParabolaMinVariation.values())  {
            System.out.println(variation.name() + " f("+P1+", "+ P2 +")="
                + variation.evaluateFitness(NumericParameterArrayTest.createParamArray(P1, P2)));
        }

        //Which evaluates to: 1.0011111
    }
}