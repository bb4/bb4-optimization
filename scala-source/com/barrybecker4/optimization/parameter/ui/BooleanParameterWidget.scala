// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.ui

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.types.BooleanParameter
import com.barrybecker4.optimization.parameter.types.Parameter
import javax.swing._
import java.awt._
import java.awt.event.ItemEvent
import java.awt.event.ItemListener


/**
  * @author Barry Becker
  */
class BooleanParameterWidget(val param: Parameter, val listener: ParameterChangeListener)
    extends ParameterWidget(param, listener) with ItemListener {

  private var cb: JCheckBox = _

  /** Create a ui widget appropriate for the parameter type. */
  override protected def addChildren(): Unit = {
    cb = new JCheckBox
    cb.setText(parameter.getName)
    val bparam = parameter.asInstanceOf[BooleanParameter]
    cb.setSelected(bparam.getNaturalValue.asInstanceOf[Boolean])
    cb.addItemListener(this)
    add(cb, BorderLayout.CENTER)
  }

  /** Called when a checkbox selection has changed for a BooleanParameter
    * @param e the item event
    */
  override def itemStateChanged(e: ItemEvent): Unit = {
    parameter.setValue(if (cb.isSelected) 1 else 0)
    doNotification()
  }

  override def refreshInternal(): Unit = {
    cb.setSelected(parameter.getNaturalValue.asInstanceOf[Boolean])
  }

  override protected def getMaxHeight = 20
}
