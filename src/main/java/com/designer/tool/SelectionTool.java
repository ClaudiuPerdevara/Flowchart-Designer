package com.designer.tool;

import com.designer.model.*;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class SelectionTool implements Tool {

    private DiagramModel model;
    private MainEditorWindow view;
    private FlowNode selectedNode;

    private double startClickX, startClickY;
    private double lastMouseX, lastMouseY;
    private boolean isActuallyDragging = false;
    private Connection draggedConnectionText = null;

    private enum HandleType { NONE, MOVE, ROTATE, NW, NE, SW, SE, CONNECT, TEXT_OFFSET, SELECT_REGION }
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
            if (currentHoveredNode != null)
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
        if (node instanceof ActorNode) {
            double cx = node.getX() + node.getWidth() / 2;
            double[][] exactAnchors = {
                    {cx, node.getY()},                                                      // Cap
                    {node.getX(), node.getY() + node.getHeight() * 0.35},                   // Mâna stângă
                    {node.getX() + node.getWidth(), node.getY() + node.getHeight() * 0.35}, // Mâna dreaptă
                    {node.getX(), node.getY() + node.getHeight()},                          // Picior stâng
                    {node.getX() + node.getWidth(), node.getY() + node.getHeight()}         // Picior drept
            };

            double bestDist = maxDist;
            double[] bestPct = null;

            for (double[] pt : exactAnchors) {
                double dist = Math.hypot(localX - pt[0], localY - pt[1]);
                if (dist <= bestDist) {
                    bestDist = dist;
                    bestPct = new double[]{ (pt[0] - node.getX()) / node.getWidth(), (pt[1] - node.getY()) / node.getHeight() };
                }
            }
            return bestPct;
        }

        double[][] vertices;
        if(node instanceof RectangleNode || node instanceof ClassNode)
        {
            vertices = new double[][]{
                    {node.getX(), node.getY()}, {node.getX() + node.getWidth(), node.getY()},
                    {node.getX() + node.getWidth(), node.getY() + node.getHeight()}, {node.getX(), node.getY() + node.getHeight()}
            };
        }
        else // Diamond
        {
            vertices = new double[][] {
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

            for(int j = 0; j < 4; j++) {
                double t = (double) j / 4.0;
                double px = sx + (ex - sx) * t;
                double py = sy + (ey - sy) * t;

                double dist = Math.hypot(localX - px, localY - py);
                if(dist <= bestDist) {
                    bestDist = dist;
                    bestPct = new double[]{ (px - node.getX()) / node.getWidth(), (py - node.getY()) / node.getHeight() };
                }
            }
        }
        return bestPct;
    }

    private double distToConection(double px, double py, double x1, double y1, double x2, double y2)
    {
        double l2 = Math.pow(x1-x2,2) + Math.pow(y1-y2,2);
        if (l2==0) return Math.hypot(px-x1, py-y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
    }

    @Override
    public void onMouseDown(MouseEvent e)
    {
        if (!e.isPrimaryButtonDown()) return;

        this.startClickX = e.getX();
        this.startClickY = e.getY();
        this.lastMouseX = e.getX();
        this.lastMouseY = e.getY();
        this.isActuallyDragging = false;

        for (Connection c : model.getConnections())
        {
            if (c.isSelected())
            {
                double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());

                double midX = (start[0] + end[0]) / 2;
                double midY = (start[1] + end[1]) / 2;
                double angleDeg = Math.toDegrees(Math.atan2(end[1] - start[1], end[0] - start[0]));
                if (angleDeg > 90) angleDeg -= 180; else if (angleDeg < -90) angleDeg += 180;

                double px = midX + c.getNameOffX();
                double py = midY + c.getNameOffY();
                double dotX = px + 16.0 * Math.sin(Math.toRadians(angleDeg));
                double dotY = py - 16.0 * Math.cos(Math.toRadians(angleDeg));

                if (Math.hypot(e.getX() - dotX, e.getY() - dotY) <= 10)
                {
                    this.handle = HandleType.TEXT_OFFSET;
                    this.draggedConnectionText = c;
                    return;
                }
            }
        }

        if (selectedNode != null && selectedNode.isSelected())
        {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
            double rad = Math.toRadians(selectedNode.getRotation());

            if (Math.hypot(e.getX() - (cx + (selectedNode.getHeight() / 2 + 30) * Math.sin(rad)), e.getY() - (cy - (selectedNode.getHeight() / 2 + 30) * Math.cos(rad))) <= 15)
            {
                handle = HandleType.ROTATE;
                return;
            }

            double angleRad = Math.toRadians(-selectedNode.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            double m = 15, nx = selectedNode.getX(), ny = selectedNode.getY(), nw = selectedNode.getWidth(), nh = selectedNode.getHeight();
            if (Math.abs(localX - nx) <= m && Math.abs(localY - ny) <= m) { handle = HandleType.NW; return; }
            else if (Math.abs(localX - (nx + nw)) <= m && Math.abs(localY - ny) <= m) { handle = HandleType.NE; return; }
            else if (Math.abs(localX - nx) <= m && Math.abs(localY - (ny + nh)) <= m) { handle = HandleType.SW; return; }
            else if (Math.abs(localX - (nx + nw)) <= m && Math.abs(localY - (ny + nh)) <= m) { handle = HandleType.SE; return; }
        }

        if (currentHoveredNode != null && !currentHoveredNode.isSelected())
        {
            double cx = currentHoveredNode.getX() + currentHoveredNode.getWidth() / 2;
            double cy = currentHoveredNode.getY() + currentHoveredNode.getHeight() / 2;
            double angleRad = Math.toRadians(-currentHoveredNode.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            // REPARATIE: Sensibilitate la ancore redusă de la 20.0 la 8.0 pentru a permite clickul pe corpul actorului
            double[] anchorPcts = getNearestAnchor(localX, localY, currentHoveredNode, 8.0);

            if (anchorPcts != null)
            {
                handle = HandleType.CONNECT;
                connectionSourceNode = currentHoveredNode;
                tempSrcPctX = anchorPcts[0];
                tempSrcPctY = anchorPcts[1];
                double[] globalStart = getGlobalCoords(currentHoveredNode.getX() + tempSrcPctX * currentHoveredNode.getWidth(), currentHoveredNode.getY() + tempSrcPctY * currentHoveredNode.getHeight(), currentHoveredNode);
                view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());
                return;
            }
        }

        Connection clickedConnection = null;
        for (Connection c : model.getConnections())
        {
            double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
            double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
            if (distToConection(e.getX(), e.getY(), start[0], start[1], end[0], end[1]) < 8.0)
            {
                clickedConnection = c;
                break;
            }
        }

        FlowNode clickedNode = model.findNodeAt(e.getX(), e.getY());

        if (clickedNode != null && e.getClickCount() == 2) { view.showInlineEditor(clickedNode); this.handle = HandleType.NONE; return; }
        if (clickedConnection != null && e.getClickCount() == 2) { view.showConnectionInlineEditor(clickedConnection); this.handle = HandleType.NONE; return; }

        if (clickedConnection != null)
        {
            for (FlowNode n : model.getNodes()) n.setSelected(false);
            for (Connection c : model.getConnections()) c.setSelected(false);
            clickedConnection.setSelected(true);
            selectedNode = null;
            handle = HandleType.NONE;
            view.showConnectionProperties(clickedConnection);
        }
        else if (clickedNode != null)
        {
            if (!clickedNode.isSelected())
            {
                for (FlowNode n : model.getNodes()) n.setSelected(false);
                for (Connection c : model.getConnections()) c.setSelected(false);
                clickedNode.setSelected(true);
            }
            selectedNode = clickedNode;
            handle = HandleType.MOVE;
            view.showNodeProperties(clickedNode);
        }
        else
        {
            for (FlowNode n : model.getNodes()) n.setSelected(false);
            for (Connection c : model.getConnections()) c.setSelected(false);
            selectedNode = null;
            handle = HandleType.SELECT_REGION;
            view.showDefaultProperties();
            view.setSelectionRegion(true, startClickX, startClickY, 0, 0);
        }

        view.drawDiagram();
    }

    @Override
    public void onMouseDragged(MouseEvent e)
    {
        this.isActuallyDragging = true;

        if (handle == HandleType.TEXT_OFFSET && draggedConnectionText != null)
        {
            double[] start = getGlobalCoords(draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth(), draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight(), draggedConnectionText.getSource());
            double[] end = getGlobalCoords(draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth(), draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight(), draggedConnectionText.getTarget());

            double vx = end[0] - start[0], vy = end[1] - start[1];
            double len = Math.hypot(vx, vy);
            if (len > 0)
            {
                double newOffX = draggedConnectionText.getNameOffX() + (e.getX() - startClickX);
                double newOffY = draggedConnectionText.getNameOffY() + (e.getY() - startClickY);
                double ux = vx / len, uy = vy / len;

                double t = newOffX * ux + newOffY * uy;
                double d = newOffX * (-uy) + newOffY * ux;

                if (t < -len*0.35) t = -len*0.35; if (t > len*0.35) t = len*0.35;
                if (d < -15) d = -15; if (d > 15) d = 15;

                draggedConnectionText.setNameOffX(t * ux + d * (-uy));
                draggedConnectionText.setNameOffY(t * uy + d * ux);
            }
            this.startClickX = e.getX();
            this.startClickY = e.getY();
            view.drawDiagram();
            return;
        }

        if (handle == HandleType.CONNECT)
        {
            double[] globalStart = getGlobalCoords(connectionSourceNode.getX() + tempSrcPctX * connectionSourceNode.getWidth(), connectionSourceNode.getY() + tempSrcPctY * connectionSourceNode.getHeight(), connectionSourceNode);
            view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());

            FlowNode target = model.findNodeAt(e.getX(), e.getY());

            if (target != null && target != connectionSourceNode)
            {
                double cx = target.getX() + target.getWidth() / 2;
                double cy = target.getY() + target.getHeight() / 2;
                double angleRad = Math.toRadians(-target.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                // Aici păstrăm 20.0 ca să fie ușor să legi liniile de omuleț
                if (getNearestAnchor(localX, localY, target, 20.0) == null)
                {
                    target = null;
                }
            }
            else
            {
                target = null;
            }

            view.setHoveredNode(target);
            view.drawDiagram();
            return;
        }

        if (handle == HandleType.SELECT_REGION)
        {
            double rx = Math.min(startClickX, e.getX());
            double ry = Math.min(startClickY, e.getY());
            double rw = Math.abs(e.getX() - startClickX);
            double rh = Math.abs(e.getY() - startClickY);

            view.setSelectionRegion(true, rx, ry, rw, rh);
            view.drawDiagram();
            return;
        }

        if (handle == HandleType.MOVE && this.selectedNode != null)
        {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            for (FlowNode n : model.getNodes())
            {
                if (n.isSelected())
                {
                    n.setX(n.getX() + dx);
                    n.setY(n.getY() + dy);
                }
            }
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            view.drawDiagram();
            return;
        }

        if (handle == HandleType.ROTATE && this.selectedNode != null)
        {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
            selectedNode.setRotation(Math.toDegrees(Math.atan2(e.getY() - cy, e.getX() - cx)) + 90);
            view.drawDiagram();
            return;
        }

        if (this.selectedNode != null)
        {
            if (handle == HandleType.NW || handle == HandleType.NE || handle == HandleType.SW || handle == HandleType.SE)
            {
                double anchorLocalX = 0, anchorLocalY = 0;
                if (handle == HandleType.SE) { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY(); }
                else if (handle == HandleType.NW) { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if (handle == HandleType.NE) { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if (handle == HandleType.SW) { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY(); }

                double[] oldGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);
                double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
                double angleRad = Math.toRadians(-selectedNode.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));
                double minSize = 20;

                // REPARAȚIE: Scalare proporțională calculată frumos doar din Unealtă!
                if (handle == HandleType.SE) {
                    double newW = localX - selectedNode.getX();
                    double newH = localY - selectedNode.getY();
                    if (selectedNode instanceof ActorNode) { newW = Math.max(newW, newH/2.0); newH = newW * 2.0; }
                    if (newW >= minSize) selectedNode.setWidth(newW);
                    if (newH >= minSize) selectedNode.setHeight(newH);
                } else if (handle == HandleType.NW) {
                    double newW = (selectedNode.getX() + selectedNode.getWidth()) - localX;
                    double newH = (selectedNode.getY() + selectedNode.getHeight()) - localY;
                    if (selectedNode instanceof ActorNode) { newW = Math.max(newW, newH/2.0); newH = newW * 2.0; localX = selectedNode.getX() + selectedNode.getWidth() - newW; localY = selectedNode.getY() + selectedNode.getHeight() - newH; }
                    if (newW >= minSize) { selectedNode.setX(localX); selectedNode.setWidth(newW); }
                    if (newH >= minSize) { selectedNode.setY(localY); selectedNode.setHeight(newH); }
                } else if (handle == HandleType.NE) {
                    double newW = localX - selectedNode.getX();
                    double newH = (selectedNode.getY() + selectedNode.getHeight()) - localY;
                    if (selectedNode instanceof ActorNode) { newW = Math.max(newW, newH/2.0); newH = newW * 2.0; localY = selectedNode.getY() + selectedNode.getHeight() - newH; }
                    if (newW >= minSize) selectedNode.setWidth(newW);
                    if (newH >= minSize) { selectedNode.setY(localY); selectedNode.setHeight(newH); }
                } else if (handle == HandleType.SW) {
                    double newW = (selectedNode.getX() + selectedNode.getWidth()) - localX;
                    double newH = localY - selectedNode.getY();
                    if (selectedNode instanceof ActorNode) { newW = Math.max(newW, newH/2.0); newH = newW * 2.0; localX = selectedNode.getX() + selectedNode.getWidth() - newW; }
                    if (newW >= minSize) { selectedNode.setX(localX); selectedNode.setWidth(newW); }
                    if (newH >= minSize) selectedNode.setHeight(newH);
                }

                double[] newGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);
                selectedNode.setX(selectedNode.getX() - (newGlobal[0] - oldGlobal[0]));
                selectedNode.setY(selectedNode.getY() - (newGlobal[1] - oldGlobal[1]));

                view.drawDiagram();
                return;
            }
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e) {

        if (handle == HandleType.SELECT_REGION) {
            double rx = Math.min(startClickX, e.getX());
            double ry = Math.min(startClickY, e.getY());
            double rw = Math.abs(e.getX() - startClickX);
            double rh = Math.abs(e.getY() - startClickY);

            if (rw > 5 && rh > 5) {
                FlowNode lastSelected = null;

                for (FlowNode n : model.getNodes()) {
                    if (n.getX() < rx + rw && n.getX() + n.getWidth() > rx &&
                            n.getY() < ry + rh && n.getY() + n.getHeight() > ry) {
                        n.setSelected(true);
                        lastSelected = n;
                    }
                }

                for (Connection c : model.getConnections()) {
                    double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                    double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                    if (start[0] >= rx && start[0] <= rx+rw && start[1] >= ry && start[1] <= ry+rh &&
                            end[0] >= rx && end[0] <= rx+rw && end[1] >= ry && end[1] <= ry+rh) {
                        c.setSelected(true);
                    }
                }

                if (lastSelected != null) {
                    view.showNodeProperties(lastSelected);
                }
            }

            view.setSelectionRegion(false, 0, 0, 0, 0);
            view.drawDiagram();
        }

        if(handle == HandleType.CONNECT) {
            FlowNode targetNode = view.getHoveredNode();
            if(targetNode != null && targetNode != connectionSourceNode) {
                double cx = targetNode.getX() + targetNode.getWidth() / 2;
                double cy = targetNode.getY() + targetNode.getHeight() / 2;
                double angleRad = Math.toRadians(-targetNode.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));
                double[] tgtAnchorPcts = getNearestAnchor(localX, localY, targetNode, 40.0);

                if(tgtAnchorPcts != null) {
                    model.addConnection(new Connection(connectionSourceNode, targetNode, tempSrcPctX, tempSrcPctY, tgtAnchorPcts[0], tgtAnchorPcts[1]));
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

    public void onMouseMoved(MouseEvent e) {
        if(handle != HandleType.NONE || view.getEditingNode() != null) return;

        FlowNode nodeUnderMouse = model.findNodeAt(e.getX(), e.getY());

        if(nodeUnderMouse != currentHoveredNode) {
            currentHoveredNode = nodeUnderMouse;
            hoverTimer.stop();
            if(currentHoveredNode != null) hoverTimer.playFromStart();
            else { view.setHoveredNode(null); view.drawDiagram(); }
        }
    }

    @Override public void onKeyPressed(KeyEvent e) {}
}