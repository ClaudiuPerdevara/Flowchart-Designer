package com.designer.tool;

import javafx.scene.input.MouseEvent;

public interface Tool {

    void onMouseDown(MouseEvent e);
    void onMouseDragged(MouseEvent e);
    void onMouseReleased(MouseEvent e);
}
