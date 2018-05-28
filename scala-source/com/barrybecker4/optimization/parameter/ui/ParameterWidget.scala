// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.ui

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.types.Parameter
import javax.swing._
import java.awt._


object ParameterWidget {
  private val MAX_WIDTH = 1000
  private val MIN_WIDTH = 200
}

/**
  * Shows UI for a given paramter
  * @author Barry Becker
  */
abstract class ParameterWidget(var parameter: Parameter, var changeListener: ParameterChangeListener) extends JPanel {

  setLayout(new BorderLayout)
  setMaximumSize(new Dimension(ParameterWidget.MAX_WIDTH, getMaxHeight))
  setMinimumSize(new Dimension(ParameterWidget.MIN_WIDTH, getMaxHeight))
  addChildren()
  private var notificationEnabled = true

  /** Make sure that the UI reflects the current parameter value, in case it has changed underneath */
  def refresh(): Unit = { // temporarily turn of notification to listeners so that we do not update listeners when
    // we modify our own internal state.
    notificationEnabled = false
    refreshInternal()
    notificationEnabled = true
  }

  def refreshInternal(): Unit

  /** Add the components to represent the parameter widget. */
  protected def addChildren(): Unit

  protected def doNotification(): Unit = {
    if (notificationEnabled) changeListener.parameterChanged(parameter)
  }

  protected def getMaxHeight = 40
}