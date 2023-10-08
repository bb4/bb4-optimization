// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import com.barrybecker4.optimization.viewer.ui.{NavigationBar, OptimizationViewable}

import java.awt.BorderLayout
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.{JComboBox, JPanel}


/**
  * Controls at the top of the OptimizerEvalFrame that allow setting the strategy and optimizee problem.
  *
  * @author Barry Becker
  */
class TopControls[P <: ParameterArray](var logFile: String, val testProblems: Seq[OptimizeeProblem[P]],
                  var viewable: OptimizationViewable[P])
     extends JPanel with ActionListener {

  private var strategyDropDown: JComboBox[OptimizationStrategyType[? <: ParameterArray]] = _
  private var testProblemDropDown: JComboBox[OptimizeeProblem[P]] = _
  private var testProblem = testProblems.head

  setLayout(new BorderLayout)
  private val navBar = new NavigationBar(viewable)
  private val comboPanel: JPanel = createStrategyCombo
  if (testProblems.length > 1) {
    testProblemDropDown = new JComboBox[OptimizeeProblem[P]](testProblems.toArray)
    testProblemDropDown.addActionListener(this)
    comboPanel.add(testProblemDropDown)
  }
  add(navBar, BorderLayout.CENTER)
  add(comboPanel, BorderLayout.EAST)
  showOptimization()


  private def createStrategyCombo = {
    val strategyPanel = new JPanel
    strategyDropDown = new JComboBox(OptimizationStrategyType.VALUES)
    strategyPanel.add(strategyDropDown)
    strategyDropDown.addActionListener(this)
    strategyPanel
  }

  override def actionPerformed(e: ActionEvent): Unit = {
    if (e.getSource eq strategyDropDown) {
      println("changed strategy to " + strategyDropDown.getSelectedItem)
      showOptimization()
    }
    if (e.getSource eq testProblemDropDown) {
      testProblem = testProblemDropDown.getSelectedItem.asInstanceOf[OptimizeeProblem[P]]
      println("changed test problem to " + testProblem)
      showOptimization()
    }
  }

  def showOptimization(): Unit = {
    val strategy = strategyDropDown.getSelectedItem.asInstanceOf[OptimizationStrategyType[P]]
    viewable.showOptimization(strategy, testProblem, logFile)
  }
}

