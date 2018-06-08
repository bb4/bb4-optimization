// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer

import java.awt.Dimension
import java.awt.event.{ActionEvent, ActionListener}
import com.barrybecker4.ui.components.{GradientButton, TexturedToolBar}
import com.barrybecker4.ui.util.GUIUtil
import javax.swing.{Box, Icon, ImageIcon}


object NavigationBar {
  private val IMAGE_PATH = "com/barrybecker4/optimization/viewer/images/"
  protected val DIR: String = IMAGE_PATH
  private val zoomInImage = GUIUtil.getIcon(DIR + "zoomIn.png")
  private val zoomOutImage = GUIUtil.getIcon(DIR + "zoomOut.png")
}

/**
  * Toolbar that appears a the top of the application window.
  * Button clicks get translated to OptimizationViewable api calls.
  * @param navListener the thing that will consume the navigation events for panning and zooming.
  * @author Barry Becker
  */
class NavigationBar(var navListener: OptimizationViewable)
  extends TexturedToolBar(GUIUtil.getIcon(NavigationBar.IMAGE_PATH + "ocean_trans_10.png")) {

  setListener(new NavBarListener)
  init()
  private var zoomInButton: GradientButton = _
  private var zoomOutButton: GradientButton = _

  private def init(): Unit = {
    zoomInButton = createToolBarButton("", "Zoom in", NavigationBar.zoomInImage)
    zoomOutButton = createToolBarButton("", "Zoom out", NavigationBar.zoomOutImage)
    add(Box.createHorizontalStrut(10))
    add(zoomInButton)
    add(zoomOutButton)
  }

  /** Create a toolbar button. */
  override def createToolBarButton(text: String, tooltip: String, icon: Icon): GradientButton = {
    val button = super.createToolBarButton(text, tooltip, icon)
    button.setMaximumSize(new Dimension(52, 52))
    button
  }

  /** handles the button clicks */
  private class NavBarListener extends ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = {
      val src = e.getSource.asInstanceOf[GradientButton]
      if (src eq zoomInButton) navListener.zoomIn()
      else if (src eq zoomOutButton) navListener.zoomOut()
    }
  }
}