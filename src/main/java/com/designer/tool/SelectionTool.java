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
                System.out.println("Rotesc!");
                return;
            }
            double angleRad=Math.toRadians(-selectedNode.getRotation());
            double dx=e.getX()-cx;
            double dy=e.getY()-cy;

            double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
            double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));

            double margin=10;
            double nodeX=selectedNode.getX();
            double nodeY=selectedNode.getY();
            double nodeH=selectedNode.getHeight();
            double nodeW=selectedNode.getWidth();

            if(Math.abs(localX-nodeX)<=margin && Math.abs(localY-nodeY)<=margin)
            {
                handle=HandleType.NW;
                System.out.print("nw");
                return;
            }
            if(Math.abs(localX-nodeX-nodeW)<=margin && Math.abs(localY-nodeY)<=margin)
            {
                handle=HandleType.NE;
                System.out.print("ne");
                return;
            }
            if(Math.abs(localX-nodeX)<=margin && Math.abs(localY-nodeY-nodeH)<=margin)
            {
                handle=HandleType.SW;
                System.out.print("sw");
                return;
            }
            if(Math.abs(localX-nodeX-nodeW)<=margin && Math.abs(localY-nodeY-nodeH)<=margin)
            {
                handle=HandleType.SE;
                System.out.print("se");
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
            else if(handle==HandleType.SE || handle==HandleType.SW || handle==HandleType.NW || handle==HandleType.NE)
            {
                double anchorLocalX=0, anchorLocalY=0;

                if(handle==HandleType.SE) { anchorLocalX=selectedNode.getX(); anchorLocalY=selectedNode.getY(); }
                else if(handle==HandleType.NW) { anchorLocalX=selectedNode.getX()+selectedNode.getWidth(); anchorLocalY= selectedNode.getY()+selectedNode.getHeight(); }
                else if(handle==HandleType.NE)  { anchorLocalX=selectedNode.getX(); anchorLocalY= selectedNode.getY()+selectedNode.getHeight(); }
                else if(handle==HandleType.SW)  { anchorLocalX=selectedNode.getX()+selectedNode.getWidth(); anchorLocalY= selectedNode.getY(); }

                double[] oldGlobal=getGlobalCoords(anchorLocalX,anchorLocalY,selectedNode);

                double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                double cy = selectedNode.getY() + selectedNode.getHeight() / 2;

                double angleRad=Math.toRadians(-selectedNode.getRotation());
                double dx=e.getX()-cx;
                double dy=e.getY()-cy;

                double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
                double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));

                double minSize=20;

                if(handle==HandleType.SE)
                {
                    double newW = localX - selectedNode.getX();
                    double newH = localY - selectedNode.getY();
                    if (newW >= minSize) selectedNode.setWidth(newW);
                    if (newH >= minSize) selectedNode.setHeight(newH);
                }

                else if(handle==HandleType.NE)
                {
                    double newW=localX-selectedNode.getX();
                    double newH=(selectedNode.getY()+selectedNode.getHeight()) -localY;

                    if(newW>=minSize)
                    {
                        selectedNode.setWidth(newW);
                    }
                    if(newH>=minSize)
                    {
                        selectedNode.setY(localY);
                        selectedNode.setHeight(newH);
                    }
                }
                else if(handle==HandleType.NW)
                {
                    double newW=(selectedNode.getX()+selectedNode.getWidth()) -localX;
                    double newH=(selectedNode.getY()+selectedNode.getHeight()) -localY;

                    if(newW>=minSize)
                    {
                        selectedNode.setX(localX);
                        selectedNode.setWidth(newW);
                    }
                    if(newH>=minSize)
                    {
                        selectedNode.setY(localY);
                        selectedNode.setHeight(newH);
                    }
                }
                else if(handle==HandleType.SW)
                {
                    double newW=(selectedNode.getX()+selectedNode.getWidth())-localX;
                    double newH=localY-selectedNode.getY();
                    if(newW>=minSize)
                    {
                        selectedNode.setY(localY);
                        selectedNode.setHeight(newH);
                    }
                }
                if (handle == HandleType.SE) { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY(); }
                else if (handle == HandleType.NW) { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if (handle == HandleType.NE) { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if (handle == HandleType.SW) { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY(); }

                double[] newGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);

                selectedNode.setX(selectedNode.getX() - (newGlobal[0] - oldGlobal[0]));
                selectedNode.setY(selectedNode.getY() - (newGlobal[1] - oldGlobal[1]));
            }

            view.drawDiagram();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e) {
        //this.selectedNode = null;
        this.handle=HandleType.NONE;
    }

    private double[] getGlobalCoords(double localPx, double localPy,FlowNode node)
    {
        double cx=node.getX()+node.getWidth()/2;
        double cy=node.getY()+node.getHeight()/2;
        double rad=Math.toRadians(node.getRotation());

        double dx=localPx-cx;
        double dy=localPy-cy;

        double globalX = cx + (dx * Math.cos(rad) - dy * Math.sin(rad));
        double globalY = cy + (dx * Math.sin(rad) + dy * Math.cos(rad));

        return new double[]{globalX, globalY};
    }
}
