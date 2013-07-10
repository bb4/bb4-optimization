// Copyright by Barry G. Becker, 2012. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;


/**
 * Implemented by something that listens for navigation events.
 * @author Barry Becker
 */
public interface NavigationListener {

    enum Direction {UP, DOWN, LEFT, RIGHT}

    void pan(Direction direction);
    void zoomIn();
    void zoomOut();
}
