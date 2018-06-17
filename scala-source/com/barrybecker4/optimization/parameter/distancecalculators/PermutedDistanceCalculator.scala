// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.Parameter
import scala.collection.mutable.ArrayBuffer


/**
  * Finds the distance between two PermutedParameterArrays.
  * @author Barry Becker
  */
class PermutedDistanceCalculator {

  /** The distance computation will be quite different for this than a regular parameter array.
    * The distance should represent a measure of the amount of similarity between two permutations,
    * taking into account symmetry. IOW, if one is the reverse of the other, then the distance is 0.
    * If there are similar runs between two permutations, then the distance should be relatively small.
    * N squared operation, where N is the number of params. The 2 param arrays must be of the same length.
    * @param pa1 first parameter array
    * @param pa2 second parameter array
    * @return the distance between this parameter array and another.
    */
  def findDistance(pa1: PermutedParameterArray, pa2: PermutedParameterArray): Double = {
    assert(pa1.size == pa2.size)
    val paReverse = pa2.reverse
    Math.min(difference(pa1, pa2), difference(pa1, paReverse))
  }

  /** The amount of difference can be used as a measure of distance.
    * @return the amount of difference between pa and ourselves.
    */
  private def difference(pa1: ParameterArray, pa2: ParameterArray) = {
    val runLengths = new ArrayBuffer[Int]
    val len = pa1.size
    var i = 0
    while (i < len) {
      val runLength = determineRunLength(pa1, pa2, len, i, runLengths)
      i += runLength
    }
    calcDistance(pa1, runLengths)
  }

  /** Adds the computed runlength to the runLengths list.
    * @return the computed runlength
    */
  private def determineRunLength(pa1: ParameterArray, pa2: ParameterArray, len: Int, idx: Int,
                                 runLengths: ArrayBuffer[Int]) = {
    var k = 0
    var i: Int = idx
    k = 1
    var j = findCorrespondingEntryIndex(pa2, len, pa1.get(idx))
    var matchFound = false
    var matched = false
    do {
      i = (i + 1) % len
      j = (j + 1) % len
      k += 1
      matched = pa1.get(i) == pa2.get(j)
      matchFound |= matched
    } while (matched && k <= len)
    val runLength = k - 1
    if (matchFound) runLengths.append(runLength)
    runLength
  }

  /** throws AssertionError if not there. It must be there.
    * @return the entry in pa that corresponds to param.
    */
  private def findCorrespondingEntryIndex(pa: ParameterArray, len: Int, param: Parameter) = {
    var j: Int = 0
    while (j < len && !(param == pa.get(j)))
      j += 1
    assert(j < len, "Param " + param + " did not match any values in " + pa)
    j
  }

  /** Find the distance between two permutations that each have runs of the specified lengths.
    * Careful this could overflow if the run is really long. If it does, we may need to switch to BigInteger.
    * @param runLengths list of run lengths.
    * @return the approximate distance between two permutations.
    */
  private def calcDistance(pa1: ParameterArray, runLengths: ArrayBuffer[Int]): Double = {
    val max = Math.pow(2, pa1.size)
    if (runLengths.isEmpty) return max
    var denom: Double = 0
    for (run <- runLengths) {
      denom += Math.pow(2, run - 1)
    }
    max / denom - 2.0
  }
}