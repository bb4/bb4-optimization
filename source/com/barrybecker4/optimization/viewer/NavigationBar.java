// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.optimization.viewer.NavigationListener.Direction;
import com.barrybecker4.ui.components.GradientButton;
import com.barrybecker4.ui.components.TexturedToolBar;
import com.barrybecker4.ui.util.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Toolbar that appears a the top of the application window.
 * @author Barry Becker
 */
public class NavigationBar extends TexturedToolBar  {

    private static final String IMAGE_PATH = "com/barrybecker4/optimization/viewer/images/";
    protected static final String DIR = IMAGE_PATH;
    private static final ImageIcon moveLeftImage = GUIUtil.getIcon(DIR + "move_left.png");
    private static final ImageIcon moveRightImage = GUIUtil.getIcon(DIR + "move_right.png");
    private static final ImageIcon moveUpImage = GUIUtil.getIcon(DIR + "move_up.png");
    private static final ImageIcon moveDownImage = GUIUtil.getIcon(DIR + "move_down.png");
    private static final ImageIcon zoomInImage = GUIUtil.getIcon(DIR + "zoomIn.png");
    private static final ImageIcon zoomOutImage = GUIUtil.getIcon(DIR + "zoomOut.png");

    private GradientButton moveLeftButton_;
    private GradientButton moveRightButton_;
    private GradientButton moveUpButton_;
    private GradientButton moveDownButton_;
    private GradientButton zoomInButton_;
    private GradientButton zoomOutButton_;

    private NavigationListener navListener;


    /**
     * Button clicks get translated to NavigationListener api calls.
     * @param listener the thing that will consume the navigation events for panning and zooming.
     */
    public NavigationBar(NavigationListener listener) {

        super(GUIUtil.getIcon(IMAGE_PATH + "ocean_trans_10.png"));
        this.setListener(new NavBarListener());
        navListener = listener;
        init();
    }

    private void init() {

        moveLeftButton_ = createToolBarButton("", "Pan left", moveLeftImage );
        moveRightButton_ = createToolBarButton("", "Pan right", moveRightImage );
        moveUpButton_ = createToolBarButton("", "Pan up", moveUpImage );
        moveDownButton_ = createToolBarButton("", "Pan down", moveDownImage );
        zoomInButton_ = createToolBarButton("", "Zoom in", zoomInImage );
        zoomOutButton_ = createToolBarButton("", "Zoom out", zoomOutImage );

        add( moveLeftButton_);
        add( moveRightButton_ );
        add( moveUpButton_ );
        add( moveDownButton_ );
        add( Box.createHorizontalStrut(10));
        add( zoomInButton_ );
        add( zoomOutButton_ );
    }

    /**
     * create a toolbar button.
     */
    @Override
    public GradientButton createToolBarButton( String text, String tooltip, Icon icon ) {
        GradientButton button = super.createToolBarButton(text, tooltip, icon);
        button.setMaximumSize( new Dimension(52, 52));
        return button;
    }

    /** handles the button clicks */
    class NavBarListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            GradientButton src = (GradientButton)  e.getSource();

            if (src == moveLeftButton_) {
                navListener.pan(Direction.LEFT);
            }
            else if (src == moveRightButton_)  {
                navListener.pan(Direction.RIGHT);
            }
            else if (src == moveUpButton_)  {
                navListener.pan(Direction.UP);
            }
            else if (src == moveDownButton_)  {
                navListener.pan(Direction.DOWN);
            }
            else if (src == zoomInButton_)  {
                navListener.zoomIn();
            }
            else if (src == zoomOutButton_)  {
                navListener.zoomOut();
            }
        }
    }
}
