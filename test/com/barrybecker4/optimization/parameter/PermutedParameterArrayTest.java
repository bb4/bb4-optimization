/** Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT  */
package com.barrybecker4.optimization.parameter;

import com.barrybecker4.common.math.MathUtil;
import com.barrybecker4.optimization.parameter.types.IntegerParameter;
import com.barrybecker4.optimization.parameter.types.Parameter;
import junit.framework.TestCase;

/**
 * @author Barry Becker
 */
public class PermutedParameterArrayTest extends TestCase {

    @Override
    public void setUp() {
        MathUtil.RANDOM.setSeed(0);
    }

    public void testPermutedNeighbor() {

        PermutedParameterArray params = createPermParameterArray(new int[] {0, 1, 2, 3, 4});
        PermutedParameterArray nbrParams = params.getRandomNeighbor(1.0);
        PermutedParameterArray expNbr =  createPermParameterArray(new int[] {3, 1, 2, 0, 4});

        assertTrue("Unexpected neighbor. expected " + expNbr +" but got "+ nbrParams,
                expNbr.equals(nbrParams));
    }

    /**
     * The two param arrays are not the same, but they are equidistant if they differ only
     * in the cyclic offset.
     */
    public void testPermutedDistanceEqual() {
        PermutedParameterArray params1 = createPermParameterArray(new int[] {2, 1, 0, 3, 4});
        PermutedParameterArray params2 = createPermParameterArray(new int[] {1, 0, 3, 4, 2});

        assertEquals("Unexpected distance", 0.0, params1.distance(params2));
    }

    /**
     * One run of length 2.
     */
    public void testPermutedDistanceAlmostEqual() {
        PermutedParameterArray params1 = createPermParameterArray(new int[] {2, 1, 0, 3, 4});
        PermutedParameterArray params2 = createPermParameterArray(new int[] {0, 1, 2, 3, 4});

        assertEquals("Unexpected distance", 6.0, params1.distance(params2));
    }

    /**
     * One run of length 3.
     */
    public void testPermutedDistanceRunLength3() {
        PermutedParameterArray params1 = createPermParameterArray(new int[] {3, 1, 0, 2, 4});
        PermutedParameterArray params2 = createPermParameterArray(new int[] {4, 1, 3, 0, 2});

        assertEquals("Unexpected distance", 6.0, params1.distance(params2));
    }


    /**
     * As different as they can be. No runs even when reversed.
     */
    public void testPermutedDistanceMaximumDifferent() {
        PermutedParameterArray params1 = createPermParameterArray(new int[] {4, 2, 0, 3, 1});
        PermutedParameterArray params2 = createPermParameterArray(new int[] {0, 1, 2, 3, 4});

        assertEquals("Unexpected distance", 32.0, params1.distance(params2));
    }

    /**
     * Two runs of length 2
     */
    public void testPermutedDistance2RunsOfLength2() {
        PermutedParameterArray params1 = createPermParameterArray(new int[] {4, 2, 0, 5, 3, 1});
        PermutedParameterArray params2 =  createPermParameterArray(new int[] {1, 5, 3, 0, 4, 2});

        // 2^6 / (2 + 2) -2
        assertEquals("Unexpected distance", 14.0, params1.distance(params2));
    }

    private PermutedParameterArray createPermParameterArray(int[] values) {

        Parameter[] params = new IntegerParameter[values.length];
        for (int i=0; i<values.length; i++) {
            params[i] = new IntegerParameter(values[i], 0, values.length-1, "param" + i);
        }

        return new PermutedParameterArray(params);
    }

}
