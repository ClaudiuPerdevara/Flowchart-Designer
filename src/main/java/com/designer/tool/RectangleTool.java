package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.model.RectangleNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.MouseEvent;

import java.awt.*;

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

        if(!e.isPrimaryButtonDown()) return;

        double width=120;
        double height=75;

        double x=e.getX()-width/2;
        double y=e.getY()-height/2;

        RectangleNode node=new RectangleNode(x,y,width,height,"");

        model.addNode(node);

        for(com.designer.model.FlowNode n : model.getNodes())
            n.setSelected(false);
        for(com.designer.model.Connection c : model.getConnections())
            c.setSelected(false);
        node.setSelected(true);

        view.showNodeProperties(node);
        view.drawDiagram();
    }
    @Override
    public void onMouseDragged(MouseEvent e) {

    }

    @Override
    public void onMouseReleased(MouseEvent e) {

    }
}
