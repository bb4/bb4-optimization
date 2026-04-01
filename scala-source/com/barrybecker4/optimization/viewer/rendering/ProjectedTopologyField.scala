package com.barrybecker4.optimization.viewer.rendering

import com.barrybecker4.math.Range
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.viewer.projectors.Projector

import scala.util.Random

case class TopologyCacheKey(
    problemName: String,
    parameterSignature: String,
    xBins: Int,
    yBins: Int
)

case class ProjectedTopologyField(
    xBins: Int,
    yBins: Int,
    xRange: Range,
    yRange: Range,
    values: Array[Double],
    normalizedValues: Array[Double],
    contourLevels: IndexedSeq[Double]
) {
  inline def idx(x: Int, y: Int): Int = y * xBins + x

  def rawX(x: Int): Double = {
    val denom = math.max(1, xBins - 1)
    xRange.min + (x.toDouble / denom) * xRange.getExtent
  }

  def rawY(y: Int): Double = {
    val denom = math.max(1, yBins - 1)
    yRange.min + (y.toDouble / denom) * yRange.getExtent
  }
}

object ProjectedTopologyField {

  private val samplingRnd = new Random(1)

  def cacheKey(problem: OptimizeeProblem, template: NumericParameterArray, xBins: Int, yBins: Int): TopologyCacheKey = {
    val sig = (0 until template.size)
      .map { i =>
        val p = template.get(i)
        s"${p.name}:${p.minValue}:${p.maxValue}:${p.isIntegerOnly}"
      }
      .mkString("|")
    TopologyCacheKey(problem.getName, sig, xBins, yBins)
  }

  def sample(
      problem: OptimizeeProblem,
      projector: Projector,
      template: NumericParameterArray,
      xBins: Int,
      yBins: Int,
      numContourLevels: Int = 10
  ): ProjectedTopologyField = {
    val xRange = projector.getXRange(template)
    val yRange = projector.getYRange(template)
    val values = Array.ofDim[Double](xBins * yBins)
    var yi = 0
    while yi < yBins do
      val v = yi.toDouble / math.max(1, yBins - 1)
      var xi = 0
      while xi < xBins do
        val u = xi.toDouble / math.max(1, xBins - 1)
        val params = buildParams(template, u, v)
        values(yi * xBins + xi) = problem.evaluateFitness(params)
        xi += 1
      yi += 1

    val normalized = normalizeRobust(values)
    val levels = (1 to math.max(1, numContourLevels)).map(i => i.toDouble / (numContourLevels + 1))
    ProjectedTopologyField(xBins, yBins, xRange, yRange, values, normalized, levels)
  }

  private def buildParams(template: NumericParameterArray, u: Double, v: Double): NumericParameterArray = {
    val params = (0 until template.size).map { i =>
      val p = template.get(i)
      val t = if (i % 2 == 0) u else v
      val raw = p.minValue + t * p.range
      clampToRange(p.setValue(raw))
    }
    NumericParameterArray(params, rnd = samplingRnd)
  }

  private def clampToRange(p: Parameter): Parameter = {
    if (p.getValue < p.minValue) p.setValue(p.minValue)
    else if (p.getValue > p.maxValue) p.setValue(p.maxValue)
    else p
  }

  private def normalizeRobust(values: Array[Double]): Array[Double] = {
    if values.isEmpty then Array.emptyDoubleArray
    else
      val sorted = values.sorted
      val lo = percentile(sorted, 0.05)
      val hi = percentile(sorted, 0.95)
      val denom = math.max(1.0e-9, hi - lo)
      values.map(v => math.max(0.0, math.min(1.0, (v - lo) / denom)))
  }

  private def percentile(sorted: Array[Double], p: Double): Double = {
    if sorted.length == 1 then sorted(0)
    else
      val idx = math.max(0, math.min(sorted.length - 1, math.round(p * (sorted.length - 1)).toInt))
      sorted(idx)
  }
}
