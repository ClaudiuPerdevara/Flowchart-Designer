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

    private enum HandleType { NONE, MOVE, TOP, RIGHT, BOTTOM, LEFT, NE, SE, SW, NW, ROTATE}
    private HandleType handle = HandleType.NONE;

    public SelectionTool(DiagramModel model, MainEditorWindow view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void onMouseDown(MouseEvent e) {

        if (selectedNode != null && selectedNode.isSelected()) {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;

            double rad = Math.toRadians(selectedNode.getRotation());
            double distToHandle = selectedNode.getHeight() / 2 + 30;


            double handleX = cx + distToHandle * Math.sin(rad);
            double handleY = cy - distToHandle * Math.cos(rad);

            double distToClick = Math.sqrt(Math.pow(e.getX() - handleX, 2) + Math.pow(e.getY() - handleY, 2));

            if (distToClick <= 15) {
                handle = HandleType.ROTATE;
                System.out.println("Am prins antena! Rotesc!");
                return;
            }
        }

        FlowNode clickedNode = model.findNodeAt(e.getX(), e.getY());

        for (FlowNode n : model.getNodes()) {
            n.setSelected(false);
        }

        if (clickedNode != null) {
            clickedNode.setSelected(true);
            selectedNode = clickedNode;
            handle = HandleType.MOVE;

            x = e.getX() - selectedNode.getX();
            y = e.getY() - selectedNode.getY();
        } else {
            selectedNode = null;
            handle = HandleType.NONE;
        }

        view.drawDiagram();
    }

    @Override
    public void onMouseDragged(MouseEvent e) {
        if (this.selectedNode != null) {

            if (handle == HandleType.ROTATE) {
                double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                double cy = selectedNode.getY() + selectedNode.getHeight() / 2;

                double angle = Math.toDegrees(Math.atan2(e.getY() - cy, e.getX() - cx));

                /// 90 de gr pt ca antena e de la ora 12 iar atan e de la ora 3
                selectedNode.setRotation(angle + 90);

            } else if (handle == HandleType.MOVE) {
                this.selectedNode.setX(e.getX() - this.x);
                this.selectedNode.setY(e.getY() - this.y);
            }

            view.drawDiagram();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e) {
        //this.selectedNode = null;
        this.handle=HandleType.NONE;
    }
}
