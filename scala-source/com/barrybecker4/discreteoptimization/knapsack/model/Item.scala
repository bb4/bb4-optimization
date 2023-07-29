package com.barrybecker4.discreteoptimization.knapsack.model

case class Item(index: Int, value: Int, weight: Int) {
  def valuePerWeight: Double = value.doubleValue / weight
}
