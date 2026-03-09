package com.designer.tool;

import com.designer.model.ActorNode;
import com.designer.model.DiagramModel;
import com.designer.model.DiamondNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.MouseEvent;

public class ActorTool implements Tool{

    private MainEditorWindow view;
    private DiagramModel model;

    public ActorTool(MainEditorWindow view, DiagramModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void onMouseDown(MouseEvent e)
    {
        if(!e.isPrimaryButtonDown()) return;

        double width=50;
        double height=120;
        double x=e.getX()-width/2;
        double y=e.getY()-height/2;

        ActorNode node= new ActorNode(x,y,width,height,"");
        model.addNode(node);

        for(com.designer.model.FlowNode flowNode:model.getNodes()) flowNode.setSelected(false);
        for(com.designer.model.Connection connection:model.getConnections()) connection.setSelected(false);
        node.setSelected(true);

        view.showNodeProperties(node);
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
