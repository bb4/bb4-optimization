// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.ui

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.ui.sliders.LabeledSlider
import com.barrybecker4.ui.sliders.SliderChangeListener
import java.awt._


/**
  * @author Barry Becker
  */
class DoubleParameterWidget(param: Parameter, listener: ParameterChangeListener)
    extends ParameterWidget(param, listener) with SliderChangeListener {
  private var slider: LabeledSlider = _

  /** Create a ui widget appropriate for the parameter type. */
  override protected def addChildren(): Unit = {
    slider = new LabeledSlider(parameter.name, parameter.getValue, parameter.minValue, parameter.maxValue)
    if (parameter.isIntegerOnly) slider.setShowAsInteger(true)
    slider.addChangeListener(this)
    add(slider, BorderLayout.CENTER)
  }

  /** @param slider the slider that changed. */
  override def sliderChanged(slider: LabeledSlider): Unit = {
    parameter.setValue(slider.getValue)
    doNotification()
  }

  override def refreshInternal(): Unit = {
    slider.setValue(parameter.getValue)
  }
}
