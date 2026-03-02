package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.model.RectangleNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.MouseEvent;

public class RectangleTool implements Tool {
    private DiagramModel model;
    private MainEditorWindow view;
    public RectangleTool(DiagramModel model, MainEditorWindow view)
    {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onMouseDown(MouseEvent e) {
        model.addNode(new RectangleNode(e.getX(),e.getY(),100,60,"Rectangle"));
        view.drawDiagram();
    }
    @Override
    public void onMouseDragged(MouseEvent e) {

    }

    @Override
    public void onMouseReleased(MouseEvent e) {

    }
}
