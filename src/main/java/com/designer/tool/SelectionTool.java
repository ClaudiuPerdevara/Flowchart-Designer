package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.DiamondNode;
import com.designer.model.FlowNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.MouseEvent;

public class SelectionTool implements Tool{

    private DiagramModel model;
    private MainEditorWindow view;
    private FlowNode selectedNode;
    private double x,y;

    public SelectionTool(DiagramModel model, MainEditorWindow view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onMouseDown(MouseEvent e) {

        this.selectedNode = model.findNodeAt(e.getX(), e.getY());
        if(selectedNode != null){
            this.x = e.getX()-selectedNode.getX();
            this.y = e.getY()-selectedNode.getY();
            System.out.println("SelectionTool: Am dat click pentru selecție la " + e.getX());
        }
    }

    @Override
    public void onMouseDragged(MouseEvent e) {
        if(this.selectedNode != null){
            this.selectedNode.setX(e.getX()-this.x);
            this.selectedNode.setY(e.getY()-this.y);

            view.drawDiagram();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e) {
        this.selectedNode = null;
    }
}
