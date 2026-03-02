package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.DiamondNode;
import com.designer.model.FlowNode;
import com.designer.model.RectangleNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.MouseEvent;

public class DiamondTool implements Tool
{
    private DiagramModel model;
    private MainEditorWindow view;

    public DiamondTool(DiagramModel model, MainEditorWindow view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onMouseDown(MouseEvent e)
    {
        model.addNode(new DiamondNode(e.getX(),e.getY(),100,60,"Diamond"));
        view.drawDiagram();
    }

    @Override
    public void onMouseDragged(MouseEvent e)
    {

    }

    @Override
    public void onMouseReleased(MouseEvent e)
    {

    }
}
