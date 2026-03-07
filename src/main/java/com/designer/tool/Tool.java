package com.designer.tool;

import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;

public interface Tool {

    void onMouseDown(MouseEvent e);
    void onMouseDragged(MouseEvent e);
    void onMouseReleased(MouseEvent e);

    default void onMouseMoved(MouseEvent e) {}
    default void onKeyPressed(KeyEvent e) {}
}
