// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.ui

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.types.StringParameter
import javax.swing._
import java.awt._
import java.awt.event.ActionEvent
import java.awt.event.ActionListener


/**
  * Discrete set of dropdown values.
  * @author Barry Becker
  */
class StringParameterWidget(val param: Parameter, val listener: ParameterChangeListener)
    extends ParameterWidget(param, listener) with ActionListener {

  private var dropdown: JComboBox[_] = _

  /**
    * Create a ui widget appropriate for the parameter type.
    */
  override protected def addChildren(): Unit = { // create a dropdown
    val sparam = parameter.asInstanceOf[StringParameter]
    dropdown = new JComboBox(sparam.getStringValues.toArray)
    dropdown.setName(parameter.name)
    dropdown.setMaximumSize(new Dimension(200, 20))
    dropdown.setToolTipText(parameter.name)
    dropdown.addActionListener(this)
    add(dropdown, BorderLayout.CENTER)
  }

  /** Called when a ComboBox selection has changed.
    * @param e the item event
    */
  override def actionPerformed(e: ActionEvent): Unit = {
    parameter = parameter.setValue(dropdown.getSelectedIndex)
    doNotification()
  }

  override def refreshInternal(): Unit = {
    dropdown.setSelectedItem(parameter.getNaturalValue)
  }

  override protected def getMaxHeight = 20
}
