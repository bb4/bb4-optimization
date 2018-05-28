// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * @author Barry Becker
  */
class MagnitudeIgnoredDistanceCalculator extends DistanceCalculator {

  /** The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two instances.
    * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
    *  - the length of the parameter array
    *  - the set of values in the parameter array.
    * Generally, the distance is greater the greater the number of parameters that are different.
    * @return the distance between this parameter array and another.
    */
  override def calculateDistance(pa1: ParameterArray, pa2: ParameterArray): Double = {
    val thisLength = pa1.size
    val thatLength = pa2.size
    var theseValues = Array.ofDim[Int](thisLength)
    var thoseValues = Array.ofDim[Int](thatLength)
    for (i <- 0 until thisLength) {
      theseValues :+= pa1.get(i).getValue.toInt
    }
    for (i <- 0 until thatLength) {
      thoseValues :+= pa2.get(i).getValue.toInt
    }
    theseValues = theseValues.sorted
    thoseValues = thoseValues.sorted

    val valueDifferences = calcValueDifferences(theseValues, thoseValues)
    Math.abs(thisLength - thatLength) + valueDifferences
  }

  /** Perform a sort of merge sort on the two sorted lists of values to find matches.
    * The more matches there are between the two lists, the more similar they are.
    * The magnitude of the differences between values does not matter, only whether they are the same or different.
    * @param theseValues first ordered list
    * @param thoseValues second ordered list
    * @return measure of the difference between the two sorted lists. It will return 0 if the two lists are the same.
    */
  private def calcValueDifferences(theseValues: Array[Int], thoseValues: Array[Int]) = {
    val thisLen: Int = theseValues.length
    val thatLen: Int = thoseValues.length
    var thisCounter: Int = 0
    var thatCounter: Int = 0
    var matchCount: Int = 0
    while (thisCounter < thisLen && thatCounter < thatLen) {
      val thisVal = theseValues(thisCounter)
      val thatVal = thoseValues(thatCounter)
      if (thisVal < thatVal) thisCounter += 1
      else if (thisVal > thatVal) thatCounter += 1
      else { // they are the same
        thisCounter += 1
        thatCounter += 1
        matchCount += 1
      }
    }
    Math.max(thisLen, thatLen) - matchCount
  }
}