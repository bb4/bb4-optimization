// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.linear.Vector
import com.barrybecker4.optimization.parameter.sampling.NumericGlobalSampler
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, Parameter}
import NumericParameterArray._
import scala.util.Random


object NumericParameterArray {
  /** default number of steps to go from the min to the max */
  private val DEFAULT_NUM_STEPS = 10

  /** The most values that [[NumericParameterArray.movesAt]] offers for one position; a bigger range is sampled. */
  val MAX_VALUES_PER_POSITION = 64

  def createParams(vals: IndexedSeq[Double], minVals: IndexedSeq[Double], maxVals: IndexedSeq[Double],
                   names: IndexedSeq[String]): IndexedSeq[Parameter] = {
    val len = vals.length
    val params = for (i <- 0 until len) yield {
      new DoubleParameter(vals(i), minVals(i), maxVals(i), names(i))
    }
    params
  }
}

/**
  * Represents a 1 dimensional array of parameters
  * @param params an array of params to initialize with.
  * @author Barry Becker
  */
case class NumericParameterArray(params: IndexedSeq[Parameter],
                                 numSteps: Int = DEFAULT_NUM_STEPS,
                                 rnd: Random = MathUtil.RANDOM)
  extends AbstractParameterArray(params, rnd) {

  /** Constructor if all the parameters are DoubleParameters
    * @param vals the values for each parameter.
    * @param minVals the minimum value allowed for each parameter respectively.
    * @param maxVals the maximum value allowed for each parameter respectively.
    * @param names   the display name for each parameter in the array.
    */
  def this(vals: IndexedSeq[Double], minVals: IndexedSeq[Double], maxVals: IndexedSeq[Double],
           names: IndexedSeq[String], rnd: Random) = {
    this(createParams(vals, minVals, maxVals, names), DEFAULT_NUM_STEPS, rnd)
  }

  /** Constructor if all the parameters are DoubleParameters
    * @param vals the values for each parameter.
    * @param minVals the minimum value allowed for each parameter respectively.
    * @param maxVals the maximum value allowed for each parameter respectively.
    * @param names   the display name for each parameter in the array.
    */
  def this(vals: Array[Double], minVals: Array[Double], maxVals: Array[Double],
    names: Array[String], rnd: Random) = {
    this(createParams(vals.toIndexedSeq, minVals.toIndexedSeq, maxVals.toIndexedSeq, names.toIndexedSeq),
      DEFAULT_NUM_STEPS, rnd)
  }

  /** Globally sample the parameter space with a uniform distribution.
    * @param requestedNumSamples approximate number of samples to retrieve. If the problem space is small
    *    and requestedNumSamples is large, it may not be possible to return this many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long): Iterator[NumericParameterArray] =
    new NumericGlobalSampler(this, requestedNumSamples)

  /** @return the distance between this parameter array and another. sqrt(sum of squares) */
  override def distance(pa: ParameterArray): Double = {
    assert(size == pa.size)
    var sumOfSq = 0.0
    for (k <- 0 until size) {
      val dif = pa.get(k).getValue - get(k).getValue
      sumOfSq += dif * dif
    }
    Math.sqrt(sumOfSq)
  }

  /** Add a vector of deltas to the parameters.
    * @param vec must be the same size as the parameter list.
    */
  def add(vec: Vector): NumericParameterArray = {
    assert(vec.size == size, "Parameter vec has magnitude " + vec.size + ", expecting " + size)

    val newParams = for (i <- 0 until size) yield {
      val param = get(i)
      clampToParameterRange(param.setValue(param.getValue + vec(i)))
    }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  /** If [[value]] lies outside the parameter's min/max, peg it and emit a warning on stderr. */
  private def clampToParameterRange(param: Parameter): Parameter = {
    var p = param
    if (p.getValue > p.maxValue) {
      Console.err.println(
        "Warning param " + p.name + " exceeds its maximum value; pegging to " + p.maxValue
      )
      p = p.setValue(p.maxValue)
    }
    if (p.getValue < p.minValue) {
      Console.err.println(
        "Warning param " + p.name + " exceeds its minimum value; pegging to " + p.minValue
      )
      p = p.setValue(p.minValue)
    }
    p
  }

  def incrementByEps(idx: Int, dir: Direction): NumericParameterArray = {
    val newParams = for (i <- 0 until size) yield {
      if (idx == i) get(i).incrementByEps(dir) else get(i)
    }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  /** @param radius the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *               (relative to each parameter range).
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): NumericParameterArray =
    NumericParameterArray(this.params.map(p => p.tweakValue(radius, rnd)), numSteps, rnd)

  /** Local moves set one integer parameter to another value. Arrays with real numbers do not have them. */
  override def supportsLocalMoves: Boolean = params.forall(_.isIntegerOnly)

  /** Setting the parameter to each other value in its range, or to a sample of them when the range is large. */
  override def movesAt(position: Int): Iterator[Move] =
    otherValues(position, rnd).iterator.map(SetValue(position, _))

  /** Set one random parameter to a different value in its range, if it has one. */
  override def proposeLocalMove(random: Random): Move = {
    val position = random.nextInt(size)
    val values = otherValues(position, random)
    SetValue(position, if (values.isEmpty) get(position).getValue else values(random.nextInt(values.size)))
  }

  override def applyMove(move: Move): NumericParameterArray = move match {
    case SetValue(index, value) => NumericParameterArray(params.updated(index, params(index).setValue(value)), numSteps, rnd)
    case other => throw new UnsupportedOperationException("A numeric array does not understand the move " + other)
  }

  override def createdAttributes(move: Move): Seq[Any] = move match {
    case SetValue(index, value) => Seq(Placement(index, value))
    case other => super.createdAttributes(other)
  }

  override def destroyedAttributes(move: Move): Seq[Any] = move match {
    case SetValue(index, _) => Seq(Placement(index, get(index).getValue))
    case other => super.destroyedAttributes(other)
  }

  /** The integer values in the range of the parameter other than its own, or at most `MAX_VALUES_PER_POSITION` of them. */
  private def otherValues(position: Int, random: Random): IndexedSeq[Double] = {
    val parameter = get(position)
    val (low, high, current) = (parameter.minValue.round.toInt, parameter.maxValue.round.toInt, parameter.getValue.round.toInt)
    val all = (low to high).filter(_ != current)
    val sampled = if (all.size <= NumericParameterArray.MAX_VALUES_PER_POSITION) all else random.shuffle(all).take(NumericParameterArray.MAX_VALUES_PER_POSITION)
    sampled.map(_.toDouble)
  }

  /** @return get a completely random solution in the parameter space.*/
  override def getRandomSample: NumericParameterArray = {
    val newParams =
      for (k <- 0 until size) yield {
        val par = get(k)
        val proposed =
          if (par.isIntegerOnly) {
            val lo = par.minValue.round.toInt
            val hi = par.maxValue.round.toInt
            val span = math.max(1, hi - lo + 1)
            (lo + rnd.nextInt(span)).toDouble
          } else par.minValue + rnd.nextDouble() * par.range
        val newPar = par.setValue(proposed)
        assert(newPar.getValue <= newPar.maxValue && newPar.getValue >= newPar.minValue,
          "newPar " + newPar.getValue + " not in [" + newPar.minValue + ", " + newPar.maxValue + "]")
        newPar
      }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  /** @return a new double array the same magnitude as the parameter list*/
  def asVector: Vector = Vector(params.map(_.getValue))
}
