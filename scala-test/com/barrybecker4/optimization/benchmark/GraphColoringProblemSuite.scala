// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import org.scalatest.funsuite.AnyFunSuite

class GraphColoringProblemSuite extends AnyFunSuite {

  test("the hidden coloring has no edge with the same color at both ends") {
    (1 to 10).foreach(seed => assertResult(0.0)(new GraphColoringProblem(60, 4, 6.0, seed).getExactSolution.fitness))
  }

  test("there are as many edges as the average degree asks for") {
    assertResult(300)(new GraphColoringProblem(100, 4, 6.0, 1).numEdges)
  }

  test("the initial guess uses several colors and clashes about as often as chance says") {
    val problem = new GraphColoringProblem(200, 4, 7.0, 2)
    val guess = problem.getInitialGuess
    assert((0 until 200).map(guess.get(_).getValue.toInt).toSet.size == 4)
    val expected = problem.numEdges / 4.0
    assert(Math.abs(problem.evaluateFitness(guess) - expected) < expected * 0.4)
  }

  test("a coloring with every node the same color clashes on every edge") {
    val problem = new GraphColoringProblem(50, 3, 4.0, 3)
    val guess = problem.getInitialGuess
    val oneColor = (0 until 50).foldLeft(guess)((g, i) => g.asInstanceOf[com.barrybecker4.optimization.parameter.NumericParameterArray]
      .copy(params = g.asInstanceOf[com.barrybecker4.optimization.parameter.NumericParameterArray].params.updated(i,
        g.get(i).setValue(0.0))))
    assertResult(problem.numEdges.toDouble)(problem.evaluateFitness(oneColor))
  }

  test("the same seed gives the same graph and the same initial guess") {
    def clashes(seed: Long) = { val p = new GraphColoringProblem(80, 4, 6.0, seed); (p.numEdges, p.evaluateFitness(p.getInitialGuess)) }
    assertResult(clashes(5))(clashes(5))
    assert(clashes(5) != clashes(6))
  }

  test("an impossible degree or too few colors are refused") {
    intercept[IllegalArgumentException](new GraphColoringProblem(10, 4, 50.0, 1))
    intercept[IllegalArgumentException](new GraphColoringProblem(10, 1, 2.0, 1))
  }
}
