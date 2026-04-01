// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

import java.awt.{BorderLayout, FlowLayout}
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.{JButton, JCheckBox, JComboBox, JLabel, JPanel}

/**
  * Controls at the top of the OptimizerEvalFrame that allow setting the strategy and optimizee problem.
  *
  * Call [[configureAndRun]] from the frame after [[OptimizationViewable.configureUiHooks]] so status/run-state wiring is active.
  *
  * @author Barry Becker
  */
class TopControls(val logFile: String, val testProblems: Seq[OptimizeeProblem], val viewable: OptimizationViewable)
    extends JPanel
    with ActionListener {

  private var testProblem: OptimizeeProblem = testProblems.head

  private val strategyDropDown = new JComboBox[OptimizationStrategyType](OptimizationStrategyType.valuesForUi)
  private val testProblemDropDown: Option[JComboBox[OptimizeeProblem]] =
    if testProblems.length > 1 then
      val box = new JComboBox[OptimizeeProblem](testProblems.toArray)
      box.addActionListener(this)
      Some(box)
    else None

  private val cancelButton = new JButton("Cancel run")
  cancelButton.setEnabled(false)
  cancelButton.addActionListener(_ => viewable.cancelCurrentOptimization())

  private val labelsCheck = new JCheckBox("Point labels", false)
  labelsCheck.addActionListener(_ => viewable.setShowPointLabels(labelsCheck.isSelected))

  private val gridCheck = new JCheckBox("Grid", true)
  gridCheck.addActionListener(_ => viewable.setShowGrid(gridCheck.isSelected))

  setLayout(new BorderLayout)
  private val navBar = new NavigationBar(viewable)
  private val comboPanel: JPanel = createStrategyCombo
  testProblemDropDown.foreach(comboPanel.add)
  comboPanel.add(new JLabel(" "))
  comboPanel.add(labelsCheck)
  comboPanel.add(gridCheck)
  comboPanel.add(cancelButton)
  add(navBar, BorderLayout.CENTER)
  add(comboPanel, BorderLayout.EAST)

  /** Wire UI hooks on the viewable, then start the first optimization run. */
  def configureAndRun(status: String => Unit, onRunStateChanged: Boolean => Unit): Unit = {
    viewable.configureUiHooks(status, onRunStateChanged)
    showOptimization()
  }

  private def createStrategyCombo: JPanel = {
    val strategyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4))
    strategyPanel.add(strategyDropDown)
    strategyDropDown.addActionListener(this)
    strategyPanel
  }

  override def actionPerformed(e: ActionEvent): Unit =
    val src = e.getSource
    if src eq strategyDropDown then showOptimization()
    else if testProblemDropDown.exists(_ eq src) then
      selectedTestProblem.foreach { p =>
        testProblem = p
        showOptimization()
      }

  private def selectedTestProblem: Option[OptimizeeProblem] =
    testProblemDropDown.flatMap(cb => Option(cb.getSelectedItem).collect { case p: OptimizeeProblem => p })

  def showOptimization(): Unit = {
    val strategy = Option(strategyDropDown.getSelectedItem) match
      case Some(s: OptimizationStrategyType) => s
      case _ =>
        throw new IllegalStateException("No optimization strategy selected")
    viewable.showOptimization(strategy, testProblem, logFile)
  }

  /** When `idle`, strategy/problem controls are enabled and Cancel is disabled. */
  def setRunChromeIdle(idle: Boolean): Unit = {
    strategyDropDown.setEnabled(idle)
    testProblemDropDown.foreach(_.setEnabled(idle))
    cancelButton.setEnabled(!idle)
  }
}
