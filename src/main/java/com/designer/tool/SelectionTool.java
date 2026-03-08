package com.designer.tool;

import com.designer.model.DiagramModel;
import com.designer.model.FlowNode;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class SelectionTool implements Tool
{

    private DiagramModel model;
    private MainEditorWindow view;
    private FlowNode selectedNode;
    private double x, y;

    private double startClickX, startClickY;
    private boolean isActuallyDragging = false;
    private com.designer.model.Connection draggedConnectionText = null;

    private enum HandleType { NONE, MOVE, ROTATE, NW, NE, SW, SE, CONNECT, TEXT_OFFSET }
    private HandleType handle = HandleType.NONE;

    private FlowNode connectionSourceNode = null;
    private double tempSrcPctX, tempSrcPctY;

    private FlowNode currentHoveredNode = null;
    private PauseTransition hoverTimer;

    public SelectionTool(DiagramModel model, MainEditorWindow view)
    {
        this.model = model;
        this.view = view;

        hoverTimer = new PauseTransition(Duration.millis(500));
        hoverTimer.setOnFinished(event ->
        {
            if(currentHoveredNode != null)
            {
                view.setHoveredNode(currentHoveredNode);
                view.drawDiagram();
            }
        });
    }

    private double[] getGlobalCoords(double localPx, double localPy, FlowNode node)
    {
        double cx = node.getX() + node.getWidth() / 2;
        double cy = node.getY() + node.getHeight() / 2;
        double rad = Math.toRadians(node.getRotation());
        double dx = localPx - cx;
        double dy = localPy - cy;
        double globalX = cx + (dx * Math.cos(rad) - dy * Math.sin(rad));
        double globalY = cy + (dx * Math.sin(rad) + dy * Math.cos(rad));
        return new double[]{globalX, globalY};
    }

    private double[] getNearestAnchor(double localX, double localY, FlowNode node, double maxDist)
    {
        double[][] vertices;
        if(node instanceof com.designer.model.RectangleNode)
        {
            vertices = new double[][]
                    {
                            {node.getX(), node.getY()}, {node.getX() + node.getWidth(), node.getY()},
                            {node.getX() + node.getWidth(), node.getY() + node.getHeight()}, {node.getX(), node.getY() + node.getHeight()}
                    };
        }
        else
        {
            vertices = new double[][]
                    {
                            {node.getX() + node.getWidth() / 2, node.getY()}, {node.getX() + node.getWidth(), node.getY() + node.getHeight() / 2},
                            {node.getX() + node.getWidth() / 2, node.getY() + node.getHeight()}, {node.getX(), node.getY() + node.getHeight() / 2}
                    };
        }

        double bestDist = maxDist;
        double[] bestPct = null;

        for(int i = 0; i < 4; i++)
        {
            double sx = vertices[i][0], sy = vertices[i][1];
            double ex = vertices[(i + 1) % 4][0], ey = vertices[(i + 1) % 4][1];

            for(int j = 0; j < 4; j++)
            {
                double t = (double) j / 4.0;
                double px = sx + (ex - sx) * t;
                double py = sy + (ey - sy) * t;

                double dist = Math.sqrt(Math.pow(localX - px, 2) + Math.pow(localY - py, 2));
                if(dist <= bestDist)
                {
                    bestDist = dist;
                    double pctX = (px - node.getX()) / node.getWidth();
                    double pctY = (py - node.getY()) / node.getHeight();
                    bestPct = new double[]{pctX, pctY};
                }
            }
        }
        return bestPct;
    }

    private double distToConection(double px, double py, double x1, double y1, double x2, double y2)
    {
        double l2=Math.pow(x1-x2,2)+Math.pow(y1-y2,2);
        if(l2==0)
            return Math.sqrt(Math.pow(px-x1,2)+Math.pow(py-y1,2));
        double t=Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        double projX=x1 + t * (x2 - x1);
        double projY=y1 + t * (y2 - y1);

        return Math.sqrt(Math.pow(px - projX, 2)+Math.pow(py - projY, 2));
    }

    @Override
    public void onMouseDown(MouseEvent e)
    {
        if (!e.isPrimaryButtonDown()) return;

        this.startClickX = e.getX();
        this.startClickY = e.getY();
        this.isActuallyDragging = false;

        for (com.designer.model.Connection c : model.getConnections())
        {
            if (c.isSelected())
            {
                double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());

                double midX = (start[0] + end[0]) / 2;
                double midY = (start[1] + end[1]) / 2;
                double angle = Math.atan2(end[1] - start[1], end[0] - start[0]);

                double angleDeg = Math.toDegrees(angle);
                if (angleDeg > 90) angleDeg -= 180;
                else if (angleDeg < -90) angleDeg += 180;
                double renderAngleRad = Math.toRadians(angleDeg);

                double px = midX + c.getNameOffX();
                double py = midY + c.getNameOffY();
                double dotOffset = 16.0;

                double dotX = px + dotOffset * Math.sin(renderAngleRad);
                double dotY = py - dotOffset * Math.cos(renderAngleRad);

                if (Math.hypot(e.getX() - dotX, e.getY() - dotY) <= 10)
                {
                    this.handle = HandleType.TEXT_OFFSET;
                    this.draggedConnectionText = c;
                    return;
                }
            }
        }

        if(selectedNode != null && selectedNode.isSelected())
        {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;

            double rad = Math.toRadians(selectedNode.getRotation());
            double distToHandle = selectedNode.getHeight() / 2 + 30;
            double handleX = cx + distToHandle * Math.sin(rad);
            double handleY = cy - distToHandle * Math.cos(rad);
            double distToClick = Math.sqrt(Math.pow(e.getX() - handleX, 2) + Math.pow(e.getY() - handleY, 2));

            if(distToClick <= 15)
            {
                handle = HandleType.ROTATE;
                return;
            }

            double angleRad = Math.toRadians(-selectedNode.getRotation());
            double dx = e.getX() - cx;
            double dy = e.getY() - cy;
            double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
            double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));

            double margin = 15;
            double nodeX = selectedNode.getX();
            double nodeY = selectedNode.getY();
            double nodeW = selectedNode.getWidth();
            double nodeH = selectedNode.getHeight();

            if(Math.abs(localX - nodeX) <= margin && Math.abs(localY - nodeY) <= margin) {
                handle = HandleType.NW; return;
            } else if(Math.abs(localX - (nodeX + nodeW)) <= margin && Math.abs(localY - nodeY) <= margin) {
                handle = HandleType.NE; return;
            } else if(Math.abs(localX - nodeX) <= margin && Math.abs(localY - (nodeY + nodeH)) <= margin) {
                handle = HandleType.SW; return;
            } else if(Math.abs(localX - (nodeX + nodeW)) <= margin && Math.abs(localY - (nodeY + nodeH)) <= margin) {
                handle = HandleType.SE; return;
            }
        }

        if (currentHoveredNode != null && !currentHoveredNode.isSelected())
        {
            double cx = currentHoveredNode.getX() + currentHoveredNode.getWidth() / 2;
            double cy = currentHoveredNode.getY() + currentHoveredNode.getHeight() / 2;
            double angleRad = Math.toRadians(-currentHoveredNode.getRotation());
            double dx = e.getX() - cx;
            double dy = e.getY() - cy;
            double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
            double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));

            double[] anchorPcts = getNearestAnchor(localX, localY, currentHoveredNode, 20.0);

            if(anchorPcts != null)
            {
                handle = HandleType.CONNECT;
                connectionSourceNode = currentHoveredNode;
                tempSrcPctX = anchorPcts[0];
                tempSrcPctY = anchorPcts[1];

                double[] globalStart = getGlobalCoords(currentHoveredNode.getX() + tempSrcPctX * currentHoveredNode.getWidth(),
                        currentHoveredNode.getY() + tempSrcPctY * currentHoveredNode.getHeight(),
                        currentHoveredNode);
                view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());
                return;
            }
        }

        final com.designer.model.Connection clickedConnection;
        {
            com.designer.model.Connection temp = null;
            for(com.designer.model.Connection c : model.getConnections())
            {
                double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());

                if (distToConection(e.getX(), e.getY(), start[0], start[1], end[0], end[1]) < 8.0)
                {
                    temp = c;
                    break;
                }
            }
            clickedConnection = temp;
        }

        FlowNode clickedNode = model.findNodeAt(e.getX(), e.getY());

        if (clickedNode != null && e.getClickCount() == 2 )
        {
            view.showInlineEditor(clickedNode);
            this.handle = HandleType.NONE;
            return;
        }

        if (clickedConnection != null && e.getClickCount() == 2)
        {
            view.showConnectionInlineEditor(clickedConnection);
            this.handle = HandleType.NONE;
            return;
        }

        for(FlowNode n : model.getNodes()) {
            n.setSelected(false);
        }
        for(com.designer.model.Connection c : model.getConnections()) {
            c.setSelected(false);
        }

        // --- AICI ESTE MAGIA: Doar spunem View-ului ce am selectat! ---
        if (clickedConnection != null)
        {
            clickedConnection.setSelected(true);
            selectedNode = null;
            handle = HandleType.NONE;
            view.showConnectionProperties(clickedConnection);
        }
        else if(clickedNode != null)
        {
            clickedNode.setSelected(true);
            selectedNode = clickedNode;
            handle = HandleType.MOVE;
            x = e.getX() - selectedNode.getX();
            y = e.getY() - selectedNode.getY();
            view.showNodeProperties(clickedNode);
        }
        else
        {
            selectedNode = null;
            handle = HandleType.NONE;
            view.showDefaultProperties();
        }

        view.drawDiagram();
    }


    @Override
    public void onMouseDragged(MouseEvent e)
    {
        this.isActuallyDragging = true;

        if (handle == HandleType.TEXT_OFFSET && draggedConnectionText != null) {
            double deltaX = e.getX() - startClickX;
            double deltaY = e.getY() - startClickY;

            double newOffX = draggedConnectionText.getNameOffX() + deltaX;
            double newOffY = draggedConnectionText.getNameOffY() + deltaY;

            double[] start = getGlobalCoords(
                    draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth(),
                    draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight(),
                    draggedConnectionText.getSource());
            double[] end = getGlobalCoords(
                    draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth(),
                    draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight(),
                    draggedConnectionText.getTarget());

            double vx = end[0] - start[0];
            double vy = end[1] - start[1];
            double len = Math.hypot(vx, vy);

            if (len > 0) {
                double ux = vx / len;
                double uy = vy / len;
                double nx = -uy;
                double ny = ux;

                double t = newOffX * ux + newOffY * uy;
                double d = newOffX * nx + newOffY * ny;

                double maxT = len * 0.35;
                if (t < -maxT) t = -maxT;
                if (t > maxT) t = maxT;

                double maxD = 15;
                if (d < -maxD) d = -maxD;
                if (d > maxD) d = maxD;

                draggedConnectionText.setNameOffX(t * ux + d * nx);
                draggedConnectionText.setNameOffY(t * uy + d * ny);
            }

            this.startClickX = e.getX();
            this.startClickY = e.getY();

            view.drawDiagram();
            return;
        }

        if(handle == HandleType.CONNECT)
        {
            double[] globalStart = getGlobalCoords(connectionSourceNode.getX() + tempSrcPctX * connectionSourceNode.getWidth(),
                    connectionSourceNode.getY() + tempSrcPctY * connectionSourceNode.getHeight(),
                    connectionSourceNode);

            view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());

            FlowNode target = null;
            for(FlowNode n : model.getNodes())
            {
                if(n == connectionSourceNode) continue;
                double cx = n.getX() + n.getWidth() / 2;
                double cy = n.getY() + n.getHeight() / 2;
                double angleRad = Math.toRadians(-n.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                if(getNearestAnchor(localX, localY, n, 40.0) != null)
                {
                    target = n;
                    break;
                }
            }
            view.setHoveredNode(target);
            view.drawDiagram();
            return;
        }

        if(this.selectedNode != null)
        {
            if(!isActuallyDragging)
            {
                if(Math.sqrt(Math.pow(e.getX() - startClickX, 2) + Math.pow(e.getY() - startClickY, 2)) > 4.0)
                {
                    isActuallyDragging = true;
                }
                else
                {
                    return;
                }
            }

            try
            {
                if(handle == HandleType.ROTATE)
                {
                    double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                    double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
                    selectedNode.setRotation(Math.toDegrees(Math.atan2(e.getY() - cy, e.getX() - cx)) + 90);
                }
                else if(handle == HandleType.MOVE)
                {
                    this.selectedNode.setX(e.getX() - this.x);
                    this.selectedNode.setY(e.getY() - this.y);
                }
                else if(handle == HandleType.NW || handle == HandleType.NE || handle == HandleType.SW || handle == HandleType.SE)
                {
                    double anchorLocalX = 0, anchorLocalY = 0;
                    if(handle == HandleType.SE)
                    {
                        anchorLocalX = selectedNode.getX();
                        anchorLocalY = selectedNode.getY();
                    }
                    else if(handle == HandleType.NW)
                    {
                        anchorLocalX = selectedNode.getX() + selectedNode.getWidth();
                        anchorLocalY = selectedNode.getY() + selectedNode.getHeight();
                    }
                    else if(handle == HandleType.NE)
                    {
                        anchorLocalX = selectedNode.getX();
                        anchorLocalY = selectedNode.getY() + selectedNode.getHeight();
                    }
                    else if(handle == HandleType.SW)
                    {
                        anchorLocalX = selectedNode.getX() + selectedNode.getWidth();
                        anchorLocalY = selectedNode.getY();
                    }

                    double[] oldGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);

                    double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                    double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
                    double angleRad = Math.toRadians(-selectedNode.getRotation());
                    double dx = e.getX() - cx;
                    double dy = e.getY() - cy;

                    double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
                    double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));
                    double minSize = 20;

                    if(handle == HandleType.SE)
                    {
                        double newW = localX - selectedNode.getX();
                        double newH = localY - selectedNode.getY();
                        if(newW >= minSize) selectedNode.setWidth(newW);
                        if(newH >= minSize) selectedNode.setHeight(newH);
                    }
                    else if(handle == HandleType.NW)
                    {
                        double newW = (selectedNode.getX() + selectedNode.getWidth()) - localX;
                        double newH = (selectedNode.getY() + selectedNode.getHeight()) - localY;
                        if(newW >= minSize)
                        {
                            selectedNode.setX(localX);
                            selectedNode.setWidth(newW);
                        }
                        if(newH >= minSize)
                        {
                            selectedNode.setY(localY);
                            selectedNode.setHeight(newH);
                        }
                    }
                    else if(handle == HandleType.NE)
                    {
                        double newW = localX - selectedNode.getX();
                        double newH = (selectedNode.getY() + selectedNode.getHeight()) - localY;
                        if(newW >= minSize) selectedNode.setWidth(newW);
                        if(newH >= minSize)
                        {
                            selectedNode.setY(localY);
                            selectedNode.setHeight(newH);
                        }
                    }
                    else if(handle == HandleType.SW)
                    {
                        double newW = (selectedNode.getX() + selectedNode.getWidth()) - localX;
                        double newH = localY - selectedNode.getY();
                        if(newW >= minSize)
                        {
                            selectedNode.setX(localX);
                            selectedNode.setWidth(newW);
                        }
                        if(newH >= minSize) selectedNode.setHeight(newH);
                    }

                    if(handle == HandleType.SE)
                    {
                        anchorLocalX = selectedNode.getX();
                        anchorLocalY = selectedNode.getY();
                    }
                    else if(handle == HandleType.NW)
                    {
                        anchorLocalX = selectedNode.getX() + selectedNode.getWidth();
                        anchorLocalY = selectedNode.getY() + selectedNode.getHeight();
                    }
                    else if(handle == HandleType.NE)
                    {
                        anchorLocalX = selectedNode.getX();
                        anchorLocalY = selectedNode.getY() + selectedNode.getHeight();
                    }
                    else if(handle == HandleType.SW)
                    {
                        anchorLocalX = selectedNode.getX() + selectedNode.getWidth();
                        anchorLocalY = selectedNode.getY();
                    }

                    double[] newGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);

                    selectedNode.setX(selectedNode.getX() - (newGlobal[0] - oldGlobal[0]));
                    selectedNode.setY(selectedNode.getY() - (newGlobal[1] - oldGlobal[1]));
                }
            }
            catch(Exception ex)
            {
            }

            view.drawDiagram();
        }
    }


    @Override
    public void onMouseReleased(MouseEvent e)
    {
        if(handle == HandleType.CONNECT)
        {
            FlowNode targetNode = view.getHoveredNode();

            if(targetNode != null && targetNode != connectionSourceNode)
            {
                double cx = targetNode.getX() + targetNode.getWidth() / 2;
                double cy = targetNode.getY() + targetNode.getHeight() / 2;
                double angleRad = Math.toRadians(-targetNode.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                double[] tgtAnchorPcts = getNearestAnchor(localX, localY, targetNode, 40.0);

                if(tgtAnchorPcts != null)
                {
                    model.addConnection(new com.designer.model.Connection(
                            connectionSourceNode, targetNode,
                            tempSrcPctX, tempSrcPctY,
                            tgtAnchorPcts[0], tgtAnchorPcts[1]
                    ));
                }
            }

            view.setTempLine(false, 0, 0, 0, 0);
            connectionSourceNode = null;
            view.setHoveredNode(null);
            view.drawDiagram();
        }

        this.isActuallyDragging = false;
        this.handle = HandleType.NONE;
    }

    public void onMouseMoved(MouseEvent e)
    {
        if(handle != HandleType.NONE) return;

        if(view.getEditingNode() != null) return;

        FlowNode nodeUnderMouse = model.findNodeAt(e.getX(), e.getY());

        if(nodeUnderMouse == null && currentHoveredNode != null)
        {
            double cx = currentHoveredNode.getX() + currentHoveredNode.getWidth() / 2;
            double cy = currentHoveredNode.getY() + currentHoveredNode.getHeight() / 2;
            double angleRad = Math.toRadians(-currentHoveredNode.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            double pad = 20.0;
            if(localX >= currentHoveredNode.getX() - pad &&
                    localX <= currentHoveredNode.getX() + currentHoveredNode.getWidth() + pad &&
                    localY >= currentHoveredNode.getY() - pad &&
                    localY <= currentHoveredNode.getY() + currentHoveredNode.getHeight() + pad)
            {
                nodeUnderMouse = currentHoveredNode;
            }
        }

        if(nodeUnderMouse != currentHoveredNode)
        {
            currentHoveredNode = nodeUnderMouse;
            hoverTimer.stop();

            if(currentHoveredNode != null)
            {
                hoverTimer.playFromStart();
            }
            else
            {
                view.setHoveredNode(null);
                view.drawDiagram();
            }
        }
    }

    @Override
    public void onKeyPressed(KeyEvent e)
    {
        if(e.getCode() == KeyCode.DELETE ||  e.getCode() == KeyCode.BACK_SPACE)
        {
            boolean needsRedraw = false;
            if(this.selectedNode != null)
            {
                model.removeNode(this.selectedNode);
                this.selectedNode = null;
                needsRedraw = true;
                this.handle = HandleType.NONE;
                view.showDefaultProperties();
            }
            else
            {
                com.designer.model.Connection connection = null;
                for(com.designer.model.Connection c : model.getConnections())
                {
                    if(c.isSelected())
                    {
                        connection = c;
                        break;
                    }
                }

                if(connection != null)
                {
                    model.removeConnection(connection);
                    view.showDefaultProperties();
                    needsRedraw = true;
                }
            }

            if(needsRedraw)
            {
                view.drawDiagram();
            }
        }
    }
}