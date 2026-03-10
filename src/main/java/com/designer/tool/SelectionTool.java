package com.designer.tool;

import com.designer.model.*;
import com.designer.view.MainEditorWindow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class SelectionTool implements Tool
{

    private DiagramModel model;
    private MainEditorWindow view;
    private FlowNode selectedNode;

    private double startClickX, startClickY;
    private double lastMouseX, lastMouseY;
    private boolean isActuallyDragging = false;
    private Connection draggedConnectionText = null;
    private double accumulatedDx = 0;
    private double accumulatedDy = 0;

    private enum HandleType { NONE, MOVE, ROTATE, NW, NE, SW, SE, CONNECT, TEXT_OFFSET, SELECT_REGION, ORTHO_H1, ORTHO_H2, ORTHO_H3 }
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
        if(node instanceof ActorNode)
        {
            double cx = node.getX() + node.getWidth() / 2;
            double[][] exactAnchors = {
                    {cx, node.getY()},
                    {node.getX(), node.getY() + node.getHeight() * 0.35},
                    {node.getX() + node.getWidth(), node.getY() + node.getHeight() * 0.35},
                    {node.getX(), node.getY() + node.getHeight()},
                    {node.getX() + node.getWidth(), node.getY() + node.getHeight()}
            };

            double bestDist = maxDist;
            double[] bestPct = null;

            for(double[] pt : exactAnchors)
            {
                double dist = Math.hypot(localX - pt[0], localY - pt[1]);
                if(dist <= bestDist)
                {
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
                double t = (double)j / 4.0;
                double px = sx + (ex - sx) * t;
                double py = sy + (ey - sy) * t;

                double dist = Math.hypot(localX - px, localY - py);
                if(dist <= bestDist)
                {
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
        if(l2==0) return Math.hypot(px-x1, py-y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        return Math.hypot(px - (x1 + t * (x2 - x1)), py - (y1 + t * (y2 - y1)));
    }

    @Override
    public void onMouseDown(MouseEvent e)
    {
        if(!e.isPrimaryButtonDown()) return;

        this.startClickX = e.getX();
        this.startClickY = e.getY();
        this.lastMouseX = e.getX();
        this.lastMouseY = e.getY();
        this.isActuallyDragging = false;
        this.accumulatedDx = 0;
        this.accumulatedDy = 0;


        // === DETECȚIE UNIFICATĂ: TEXT, CERCULEȚ ȘI MÂNERE ===
        for (Connection c : model.getConnections()) {
            if (c.isSelected()) {
                boolean isOrtho = c.getLineStyle() == Connection.LineStyle.ORTHOGONAL || c.getLineStyle() == Connection.LineStyle.ORTHOGONAL_DASHED;
                boolean isHVH = true;
                if (isOrtho) {
                    double dx = Math.min(c.getSrcPctX(), 1.0 - c.getSrcPctX());
                    double dy = Math.min(c.getSrcPctY(), 1.0 - c.getSrcPctY());
                    if (dy < dx) isHVH = false;
                }

                double[] start, end;
                if (isOrtho) {
                    if (isHVH) {
                        double faceSrcX = (c.getSource().getX() < c.getTarget().getX()) ? 1.0 : 0.0;
                        double faceTgtX = (c.getSource().getX() < c.getTarget().getX()) ? 0.0 : 1.0;
                        start = getGlobalCoords(c.getSource().getX() + faceSrcX * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                        end = getGlobalCoords(c.getTarget().getX() + faceTgtX * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                    } else {
                        double faceSrcY = (c.getSource().getY() < c.getTarget().getY()) ? 1.0 : 0.0;
                        double faceTgtY = (c.getSource().getY() < c.getTarget().getY()) ? 0.0 : 1.0;
                        start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + faceSrcY * c.getSource().getHeight(), c.getSource());
                        end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + faceTgtY * c.getTarget().getHeight(), c.getTarget());
                    }
                } else {
                    start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                    end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                }

                double sx = start[0], sy = start[1], ex = end[0], ey = end[1];
                double midX = (sx + ex) / 2;
                double midY = (sy + ey) / 2;

                if (isOrtho) {
                    if (isHVH) {
                        double maxOffset = Math.abs(sx - ex) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                        midX += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));
                    } else {
                        double maxOffset = Math.abs(sy - ey) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                        midY += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));
                    }
                }

                // 1. Verificăm click pe Mânere
                if (isOrtho) {
                    double h1x, h1y, h2x, h2y, h3x, h3y;
                    if (isHVH) {
                        h1x = sx + (midX - sx)/2; h1y = sy; h2x = midX; h2y = (sy + ey)/2; h3x = midX + (ex - midX)/2; h3y = ey;
                    } else {
                        h1x = sx; h1y = sy + (midY - sy)/2; h2x = (sx + ex)/2; h2y = midY; h3x = ex; h3y = midY + (ey - midY)/2;
                    }
                    if (Math.hypot(e.getX() - h1x, e.getY() - h1y) <= 8) { this.handle = HandleType.ORTHO_H1; this.draggedConnectionText = c; return; }
                    if (Math.hypot(e.getX() - h2x, e.getY() - h2y) <= 8) { this.handle = HandleType.ORTHO_H2; this.draggedConnectionText = c; return; }
                    if (Math.hypot(e.getX() - h3x, e.getY() - h3y) <= 8) { this.handle = HandleType.ORTHO_H3; this.draggedConnectionText = c; return; }
                }

                // 2. Verificăm click pe Text/Cerculeț
                double textX = midX + c.getNameOffX();
                double textY = midY + c.getNameOffY();
                double angleDeg = 0;

                if (isOrtho) {
                    if (isHVH) { textX += 15; angleDeg = 0; } else { textY -= 15; angleDeg = 0; }
                } else {
                    angleDeg = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
                    if (angleDeg > 90) angleDeg -= 180; else if (angleDeg < -90) angleDeg += 180;
                }

                double rad = Math.toRadians(angleDeg);
                double dotX = textX + 15.5 * Math.sin(rad);
                double dotY = textY - 15.5 * Math.cos(rad);

                if (Math.hypot(e.getX() - dotX, e.getY() - dotY) <= 12) {
                    this.handle = HandleType.TEXT_OFFSET;
                    this.draggedConnectionText = c;
                    return;
                }
            }
        }

        // === DETECȚIE CLICK PE LINIE ===
        Connection clickedConnection = null;
        for (Connection c : model.getConnections()) {
            boolean isOrtho = c.getLineStyle() == Connection.LineStyle.ORTHOGONAL || c.getLineStyle() == Connection.LineStyle.ORTHOGONAL_DASHED;
            boolean isHVH = true;
            if (isOrtho) {
                double dx = Math.min(c.getSrcPctX(), 1.0 - c.getSrcPctX());
                double dy = Math.min(c.getSrcPctY(), 1.0 - c.getSrcPctY());
                if (dy < dx) isHVH = false;
            }

            double[] start, end;
            if (isOrtho) {
                if (isHVH) {
                    double faceSrcX = (c.getSource().getX() < c.getTarget().getX()) ? 1.0 : 0.0;
                    double faceTgtX = (c.getSource().getX() < c.getTarget().getX()) ? 0.0 : 1.0;
                    start = getGlobalCoords(c.getSource().getX() + faceSrcX * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                    end = getGlobalCoords(c.getTarget().getX() + faceTgtX * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                } else {
                    double faceSrcY = (c.getSource().getY() < c.getTarget().getY()) ? 1.0 : 0.0;
                    double faceTgtY = (c.getSource().getY() < c.getTarget().getY()) ? 0.0 : 1.0;
                    start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + faceSrcY * c.getSource().getHeight(), c.getSource());
                    end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + faceTgtY * c.getTarget().getHeight(), c.getTarget());
                }
            } else {
                start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
            }

            if (isOrtho) {
                double sx = start[0], sy = start[1], ex = end[0], ey = end[1];
                double midX = (sx + ex) / 2;
                double midY = (sy + ey) / 2;

                double dist1, dist2, dist3;
                if (isHVH) {
                    double maxOffset = Math.abs(sx - ex) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                    midX += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));

                    dist1 = distToConection(e.getX(), e.getY(), sx, sy, midX, sy);
                    dist2 = distToConection(e.getX(), e.getY(), midX, sy, midX, ey);
                    dist3 = distToConection(e.getX(), e.getY(), midX, ey, ex, ey);
                } else {
                    double maxOffset = Math.abs(sy - ey) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                    midY += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));

                    dist1 = distToConection(e.getX(), e.getY(), sx, sy, sx, midY);
                    dist2 = distToConection(e.getX(), e.getY(), sx, midY, ex, midY);
                    dist3 = distToConection(e.getX(), e.getY(), ex, midY, ex, ey);
                }
                if (Math.min(dist1, Math.min(dist2, dist3)) < 8.0) { clickedConnection = c; break; }
            } else {
                if (distToConection(e.getX(), e.getY(), start[0], start[1], end[0], end[1]) < 8.0) { clickedConnection = c; break; }
            }
        }

        if(selectedNode != null && selectedNode.isSelected())
        {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
            double rad = Math.toRadians(selectedNode.getRotation());

            if(Math.hypot(e.getX() - (cx + (selectedNode.getHeight() / 2 + 30) * Math.sin(rad)), e.getY() - (cy - (selectedNode.getHeight() / 2 + 30) * Math.cos(rad))) <= 15)
            {
                handle = HandleType.ROTATE;
                return;
            }

            double angleRad = Math.toRadians(-selectedNode.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            double m = 10, nx = selectedNode.getX(), ny = selectedNode.getY(), nw = selectedNode.getWidth(), nh = selectedNode.getHeight();
            if(Math.abs(localX - nx) <= m && Math.abs(localY - ny) <= m)
            { handle = HandleType.NW; return; }
            else if(Math.abs(localX - (nx + nw)) <= m && Math.abs(localY - ny) <= m)
            { handle = HandleType.NE; return; }
            else if(Math.abs(localX - nx) <= m && Math.abs(localY - (ny + nh)) <= m)
            { handle = HandleType.SW; return; }
            else if(Math.abs(localX - (nx + nw)) <= m && Math.abs(localY - (ny + nh)) <= m)
            { handle = HandleType.SE; return; }
        }

        FlowNode visuallyHovered = view.getHoveredNode();
        if (visuallyHovered != null && !visuallyHovered.isSelected())
        {
            double cx = visuallyHovered.getX() + visuallyHovered.getWidth() / 2;
            double cy = visuallyHovered.getY() + visuallyHovered.getHeight() / 2;
            double angleRad = Math.toRadians(-visuallyHovered.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            double[] anchorPcts = getNearestAnchor(localX, localY, visuallyHovered, 14.0);

            if (anchorPcts != null)
            {
                handle = HandleType.CONNECT;
                connectionSourceNode = visuallyHovered;
                tempSrcPctX = anchorPcts[0];
                tempSrcPctY = anchorPcts[1];
                double[] globalStart = getGlobalCoords(visuallyHovered.getX() + tempSrcPctX * visuallyHovered.getWidth(), visuallyHovered.getY() + tempSrcPctY * visuallyHovered.getHeight(), visuallyHovered);
                view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());
                return;
            }
        }

        for (Connection c : model.getConnections()) {
            boolean isOrtho = c.getLineStyle() == Connection.LineStyle.ORTHOGONAL || c.getLineStyle() == Connection.LineStyle.ORTHOGONAL_DASHED;
            boolean isHVH = true;
            if (isOrtho && (c.getSrcPctY() <= 0.05 || c.getSrcPctY() >= 0.95) && (c.getSrcPctX() > 0.05 && c.getSrcPctX() < 0.95)) isHVH = false;

            double[] start, end;
            if (isOrtho) {
                if (isHVH) {
                    double faceSrcX = (c.getSource().getX() < c.getTarget().getX()) ? 1.0 : 0.0;
                    double faceTgtX = (c.getSource().getX() < c.getTarget().getX()) ? 0.0 : 1.0;
                    start = getGlobalCoords(c.getSource().getX() + faceSrcX * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                    end = getGlobalCoords(c.getTarget().getX() + faceTgtX * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                } else {
                    double faceSrcY = (c.getSource().getY() < c.getTarget().getY()) ? 1.0 : 0.0;
                    double faceTgtY = (c.getSource().getY() < c.getTarget().getY()) ? 0.0 : 1.0;
                    start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + faceSrcY * c.getSource().getHeight(), c.getSource());
                    end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + faceTgtY * c.getTarget().getHeight(), c.getTarget());
                }
            } else {
                start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
            }

            if (isOrtho) {
                double sx = start[0], sy = start[1], ex = end[0], ey = end[1];
                double midX = (sx + ex) / 2;
                double midY = (sy + ey) / 2;

                double dist1, dist2, dist3;
                if (isHVH) {
                    double maxOffset = Math.abs(sx - ex) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                    midX += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));

                    dist1 = distToConection(e.getX(), e.getY(), sx, sy, midX, sy);
                    dist2 = distToConection(e.getX(), e.getY(), midX, sy, midX, ey);
                    dist3 = distToConection(e.getX(), e.getY(), midX, ey, ex, ey);
                } else {
                    double maxOffset = Math.abs(sy - ey) / 2 - 20; if (maxOffset < 0) maxOffset = 0;
                    midY += Math.max(-maxOffset, Math.min(maxOffset, c.getOrthoOffset()));

                    dist1 = distToConection(e.getX(), e.getY(), sx, sy, sx, midY);
                    dist2 = distToConection(e.getX(), e.getY(), sx, midY, ex, midY);
                    dist3 = distToConection(e.getX(), e.getY(), ex, midY, ex, ey);
                }
                if (Math.min(dist1, Math.min(dist2, dist3)) < 8.0) { clickedConnection = c; break; }
            } else {
                if (distToConection(e.getX(), e.getY(), start[0], start[1], end[0], end[1]) < 8.0) { clickedConnection = c; break; }
            }
        }

        FlowNode clickedNode = model.findNodeAt(e.getX(), e.getY());

        if(clickedNode != null && e.getClickCount() == 2)
        {
            double cx = clickedNode.getX() + clickedNode.getWidth() / 2;
            double cy = clickedNode.getY() + clickedNode.getHeight() / 2;
            double angleRad = Math.toRadians(-clickedNode.getRotation());
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            double relativeClickY = localY - clickedNode.getY();

            view.showInlineEditor(clickedNode, relativeClickY);
            this.handle = HandleType.NONE;
            return;
        }

        if(clickedConnection != null && e.getClickCount() == 2)
        {
            view.showConnectionInlineEditor(clickedConnection);
            this.handle = HandleType.NONE;
            return;
        }

        if(clickedConnection != null)
        {
            for(FlowNode n : model.getNodes()) n.setSelected(false);
            for(Connection c : model.getConnections()) c.setSelected(false);
            clickedConnection.setSelected(true);
            selectedNode = null;
            handle = HandleType.NONE;
            view.showConnectionProperties(clickedConnection);
        }
        else if(clickedNode != null)
        {
            if(!clickedNode.isSelected())
            {
                for(FlowNode n : model.getNodes()) n.setSelected(false);
                for(Connection c : model.getConnections()) c.setSelected(false);
                clickedNode.setSelected(true);
            }
            selectedNode = clickedNode;
            handle = HandleType.MOVE;
            view.showNodeProperties(clickedNode);
        }
        else
        {
            for(FlowNode n : model.getNodes()) n.setSelected(false);
            for(Connection c : model.getConnections()) c.setSelected(false);
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

        // === RUTARE AVANSATĂ: TRAGEREA CELOR 3 SEGMENTE UML ===
        if (handle == HandleType.TEXT_OFFSET && draggedConnectionText != null) {
            double dx = e.getX() - lastMouseX; double dy = e.getY() - lastMouseY;
            double newOffX = draggedConnectionText.getNameOffX() + dx;
            double newOffY = draggedConnectionText.getNameOffY() + dy;

            boolean isOrtho = draggedConnectionText.getLineStyle() == Connection.LineStyle.ORTHOGONAL || draggedConnectionText.getLineStyle() == Connection.LineStyle.ORTHOGONAL_DASHED;

            if (isOrtho) {
                double maxDistance = 40.0;
                double currentDist = Math.hypot(newOffX, newOffY);
                if (currentDist > maxDistance) { newOffX = (newOffX / currentDist) * maxDistance; newOffY = (newOffY / currentDist) * maxDistance; }
            } else {
                double[] start = getGlobalCoords(draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth(), draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight(), draggedConnectionText.getSource());
                double[] end = getGlobalCoords(draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth(), draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight(), draggedConnectionText.getTarget());
                double vx = end[0] - start[0]; double vy = end[1] - start[1]; double len = Math.hypot(vx, vy);
                if (len > 0) {
                    double ux = vx / len; double uy = vy / len;
                    double t = newOffX * ux + newOffY * uy; double d = newOffX * (-uy) + newOffY * ux;
                    double maxD = 40.0; if (d < -maxD) d = -maxD; if (d > maxD) d = maxD;
                    double maxT = Math.max(0, (len / 2) - 35); if (t < -maxT) t = -maxT; if (t > maxT) t = maxT;
                    newOffX = t * ux + d * (-uy); newOffY = t * uy + d * ux;
                }
            }

            draggedConnectionText.setNameOffX(newOffX); draggedConnectionText.setNameOffY(newOffY);
            this.lastMouseX = e.getX(); this.lastMouseY = e.getY(); view.drawDiagram(); return;
        }

        // === RUTARE AVANSATĂ: TRAGEREA CELOR 3 SEGMENTE UML ===
        if ((handle == HandleType.ORTHO_H1 || handle == HandleType.ORTHO_H2 || handle == HandleType.ORTHO_H3) && draggedConnectionText != null) {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            boolean isHVH = true;
            double pctDx = Math.min(draggedConnectionText.getSrcPctX(), 1.0 - draggedConnectionText.getSrcPctX());
            double pctDy = Math.min(draggedConnectionText.getSrcPctY(), 1.0 - draggedConnectionText.getSrcPctY());
            if (pctDy < pctDx) isHVH = false;

            if (handle == HandleType.ORTHO_H2) {
                double delta = isHVH ? dx : dy;
                double newOffset = draggedConnectionText.getOrthoOffset() + delta;

                double[] start, end;
                if (isHVH) {
                    double faceSrcX = (draggedConnectionText.getSource().getX() < draggedConnectionText.getTarget().getX()) ? 1.0 : 0.0;
                    double faceTgtX = (draggedConnectionText.getSource().getX() < draggedConnectionText.getTarget().getX()) ? 0.0 : 1.0;
                    start = getGlobalCoords(draggedConnectionText.getSource().getX() + faceSrcX * draggedConnectionText.getSource().getWidth(), draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight(), draggedConnectionText.getSource());
                    end = getGlobalCoords(draggedConnectionText.getTarget().getX() + faceTgtX * draggedConnectionText.getTarget().getWidth(), draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight(), draggedConnectionText.getTarget());
                } else {
                    double faceSrcY = (draggedConnectionText.getSource().getY() < draggedConnectionText.getTarget().getY()) ? 1.0 : 0.0;
                    double faceTgtY = (draggedConnectionText.getSource().getY() < draggedConnectionText.getTarget().getY()) ? 0.0 : 1.0;
                    start = getGlobalCoords(draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth(), draggedConnectionText.getSource().getY() + faceSrcY * draggedConnectionText.getSource().getHeight(), draggedConnectionText.getSource());
                    end = getGlobalCoords(draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth(), draggedConnectionText.getTarget().getY() + faceTgtY * draggedConnectionText.getTarget().getHeight(), draggedConnectionText.getTarget());
                }

                double maxLimit = isHVH ? (Math.abs(start[0] - end[0]) / 2 - 20) : (Math.abs(start[1] - end[1]) / 2 - 20);
                if (maxLimit < 0) maxLimit = 0;

                if (newOffset < -maxLimit) newOffset = -maxLimit;
                if (newOffset > maxLimit) newOffset = maxLimit;

                draggedConnectionText.setOrthoOffset(newOffset);
            }
            else if (handle == HandleType.ORTHO_H1) {
                if (isHVH) {
                    double newY = (draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight()) + dy;
                    double pct = (newY - draggedConnectionText.getSource().getY()) / draggedConnectionText.getSource().getHeight();
                    draggedConnectionText.setSrcPctY(Math.max(0, Math.min(1, pct)));
                } else {
                    double newX = (draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth()) + dx;
                    double pct = (newX - draggedConnectionText.getSource().getX()) / draggedConnectionText.getSource().getWidth();
                    draggedConnectionText.setSrcPctX(Math.max(0, Math.min(1, pct)));
                }
            }
            else if (handle == HandleType.ORTHO_H3) {
                if (isHVH) {
                    double newY = (draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight()) + dy;
                    double pct = (newY - draggedConnectionText.getTarget().getY()) / draggedConnectionText.getTarget().getHeight();
                    draggedConnectionText.setTgtPctY(Math.max(0, Math.min(1, pct)));
                } else {
                    double newX = (draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth()) + dx;
                    double pct = (newX - draggedConnectionText.getTarget().getX()) / draggedConnectionText.getTarget().getWidth();
                    draggedConnectionText.setTgtPctX(Math.max(0, Math.min(1, pct)));
                }
            }

            this.lastMouseX = e.getX(); this.lastMouseY = e.getY(); view.drawDiagram(); return;
        }

        if (handle == HandleType.CONNECT) {
            double[] globalStart = getGlobalCoords(connectionSourceNode.getX() + tempSrcPctX * connectionSourceNode.getWidth(), connectionSourceNode.getY() + tempSrcPctY * connectionSourceNode.getHeight(), connectionSourceNode);
            view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());

            FlowNode target = model.findNodeAt(e.getX(), e.getY());

            // NOU: Daca nu suntem FIX pe forma, cautam în raza de 20px (Hover Padding)!
            if (target == null) {
                for (FlowNode n : model.getNodes()) {
                    if (n == connectionSourceNode) continue;
                    double cx = n.getX() + n.getWidth() / 2;
                    double cy = n.getY() + n.getHeight() / 2;
                    double angleRad = Math.toRadians(-n.getRotation());
                    double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                    double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                    if (localX >= n.getX() - 20 && localX <= n.getX() + n.getWidth() + 20 &&
                            localY >= n.getY() - 20 && localY <= n.getY() + n.getHeight() + 20) {
                        target = n; break;
                    }
                }
            }

            if (target != null && target != connectionSourceNode) {
                double cx = target.getX() + target.getWidth() / 2;
                double cy = target.getY() + target.getHeight() / 2;
                double angleRad = Math.toRadians(-target.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                if (getNearestAnchor(localX, localY, target, 40.0) == null) target = null;
            } else { target = null; }

            view.setHoveredNode(target); view.drawDiagram(); return;
        }

        if (handle == HandleType.TEXT_OFFSET && draggedConnectionText != null)
        {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            double newOffX = draggedConnectionText.getNameOffX() + dx;
            double newOffY = draggedConnectionText.getNameOffY() + dy;

            boolean isOrtho = draggedConnectionText.getLineStyle() == Connection.LineStyle.ORTHOGONAL ||
                    draggedConnectionText.getLineStyle() == Connection.LineStyle.ORTHOGONAL_DASHED;

            if (isOrtho)
            {
                double maxDistance = 40.0;
                double currentDist = Math.hypot(newOffX, newOffY);
                if (currentDist > maxDistance)
                {
                    newOffX = (newOffX / currentDist) * maxDistance;
                    newOffY = (newOffY / currentDist) * maxDistance;
                }
            }
            else
            {
                double[] start = getGlobalCoords(draggedConnectionText.getSource().getX() + draggedConnectionText.getSrcPctX() * draggedConnectionText.getSource().getWidth(), draggedConnectionText.getSource().getY() + draggedConnectionText.getSrcPctY() * draggedConnectionText.getSource().getHeight(), draggedConnectionText.getSource());
                double[] end = getGlobalCoords(draggedConnectionText.getTarget().getX() + draggedConnectionText.getTgtPctX() * draggedConnectionText.getTarget().getWidth(), draggedConnectionText.getTarget().getY() + draggedConnectionText.getTgtPctY() * draggedConnectionText.getTarget().getHeight(), draggedConnectionText.getTarget());

                double vx = end[0] - start[0];
                double vy = end[1] - start[1];
                double len = Math.hypot(vx, vy);

                if (len > 0) {
                    double ux = vx / len;
                    double uy = vy / len;

                    double t = newOffX * ux + newOffY * uy; // t = deplasarea stânga/dreapta (paralel)
                    double d = newOffX * (-uy) + newOffY * ux; // d = deplasarea sus/jos (perpendicular)

                    // Limite SUS / JOS (Permitem până la 40px distanță față de linie)
                    double maxD = 20.0;
                    if (d < -maxD) d = -maxD;
                    if (d > maxD) d = maxD;

                    double maxT = Math.max(0, (len / 2) - 35);
                    if (t < -maxT) t = -maxT;
                    if (t > maxT) t = maxT;

                    newOffX = t * ux + d * (-uy);
                    newOffY = t * uy + d * ux;
                }
            }

            draggedConnectionText.setNameOffX(newOffX);
            draggedConnectionText.setNameOffY(newOffY);

            this.lastMouseX = e.getX();
            this.lastMouseY = e.getY();
            view.drawDiagram();
            return;
        }

        if(handle == HandleType.CONNECT)
        {
            double[] globalStart = getGlobalCoords(connectionSourceNode.getX() + tempSrcPctX * connectionSourceNode.getWidth(), connectionSourceNode.getY() + tempSrcPctY * connectionSourceNode.getHeight(), connectionSourceNode);
            view.setTempLine(true, globalStart[0], globalStart[1], e.getX(), e.getY());

            FlowNode target = model.findNodeAt(e.getX(), e.getY());

            if(target != null && target != connectionSourceNode)
            {
                double cx = target.getX() + target.getWidth() / 2;
                double cy = target.getY() + target.getHeight() / 2;
                double angleRad = Math.toRadians(-target.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                if(getNearestAnchor(localX, localY, target, 20.0) == null)
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

        if(handle == HandleType.SELECT_REGION)
        {
            double rx = Math.min(startClickX, e.getX());
            double ry = Math.min(startClickY, e.getY());
            double rw = Math.abs(e.getX() - startClickX);
            double rh = Math.abs(e.getY() - startClickY);

            view.setSelectionRegion(true, rx, ry, rw, rh);
            view.drawDiagram();
            return;
        }

        if(handle == HandleType.MOVE && this.selectedNode != null)
        {
            double dx = e.getX() - lastMouseX;
            double dy = e.getY() - lastMouseY;

            if(view.isGridVisible())
            {
                accumulatedDx+=dx;
                accumulatedDy+=dy;
                double grid=20.0;
                double snapDx=Math.round(accumulatedDx/grid)*grid;
                double snapDy=Math.round(accumulatedDy/grid)*grid;

                if (snapDx != 0 || snapDy != 0)
                {
                    for (FlowNode n : model.getNodes())
                    {
                        if (n.isSelected())
                        {
                            n.setX(Math.round((n.getX() + snapDx) / grid) * grid);
                            n.setY(Math.round((n.getY() + snapDy) / grid) * grid);
                        }
                    }
                    accumulatedDx -= snapDx;
                    accumulatedDy -= snapDy;
                }
            }
            else
            {
                for(FlowNode n : model.getNodes())
                {
                    if(n.isSelected())
                    {
                        n.setX(n.getX() + dx);
                        n.setY(n.getY() + dy);
                    }
                }
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            view.drawDiagram();
            return;
        }

        if(handle == HandleType.ROTATE && this.selectedNode != null)
        {
            double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
            double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
            selectedNode.setRotation(Math.toDegrees(Math.atan2(e.getY() - cy, e.getX() - cx)) + 90);
            view.drawDiagram();
            return;
        }

        if(this.selectedNode != null)
        {
            if(handle == HandleType.NW || handle == HandleType.NE || handle == HandleType.SW || handle == HandleType.SE)
            {
                if(selectedNode instanceof ActorNode)
                {
                    double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                    double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
                    double angleRad = Math.toRadians(-selectedNode.getRotation());
                    double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                    double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

                    double minSize = 30;
                    double originalW = selectedNode.getWidth();
                    double originalH = selectedNode.getHeight();
                    double newW, newH;

                    if(handle == HandleType.SE)
                    {
                        newW = localX - selectedNode.getX();
                        newH = localY - selectedNode.getY();
                    }
                    else if(handle == HandleType.NW)
                    {
                        newW = (selectedNode.getX() + originalW) - localX;
                        newH = (selectedNode.getY() + originalH) - localY;
                    }
                    else if(handle == HandleType.NE)
                    {
                        newW = localX - selectedNode.getX();
                        newH = (selectedNode.getY() + originalH) - localY;
                    }
                    else
                    {
                        newW = (selectedNode.getX() + originalW) - localX;
                        newH = localY - selectedNode.getY();
                    }

                    double scaleW = newW / originalW;
                    double scaleH = newH / originalH;
                    double scale = Math.max(scaleW, scaleH);

                    if(scale >= minSize / originalW)
                    {
                        double finalW = originalW * scale;
                        double finalH = originalH * scale;

                        double[] oldGlobal = getGlobalCoords(selectedNode.getX() + originalW / 2, selectedNode.getY() + originalH / 2, selectedNode);
                        selectedNode.setWidth(finalW);
                        selectedNode.setHeight(finalH);

                        if(handle == HandleType.SE)
                        {
                        }
                        else if(handle == HandleType.NW)
                        {
                            selectedNode.setX(selectedNode.getX() + (originalW - finalW));
                            selectedNode.setY(selectedNode.getY() + (originalH - finalH));
                        }
                        else if(handle == HandleType.NE)
                        {
                            selectedNode.setY(selectedNode.getY() + (originalH - finalH));
                        }
                        else if(handle == HandleType.SW)
                        {
                            selectedNode.setX(selectedNode.getX() + (originalW - finalW));
                        }
                    }
                    view.drawDiagram();
                    return;
                }

                double anchorLocalX = 0, anchorLocalY = 0;
                if(handle == HandleType.SE)
                { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY(); }
                else if(handle == HandleType.NW)
                { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if(handle == HandleType.NE)
                { anchorLocalX = selectedNode.getX(); anchorLocalY = selectedNode.getY() + selectedNode.getHeight(); }
                else if(handle == HandleType.SW)
                { anchorLocalX = selectedNode.getX() + selectedNode.getWidth(); anchorLocalY = selectedNode.getY(); }

                double[] oldGlobal = getGlobalCoords(anchorLocalX, anchorLocalY, selectedNode);
                double cx = selectedNode.getX() + selectedNode.getWidth() / 2;
                double cy = selectedNode.getY() + selectedNode.getHeight() / 2;
                double angleRad = Math.toRadians(-selectedNode.getRotation());
                double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
                double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));
                double minSize = 20;

                if (view.isGridVisible())
                {
                    localX = Math.round(localX / 20.0) * 20.0;
                    localY = Math.round(localY / 20.0) * 20.0;
                }

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
                    { selectedNode.setX(localX); selectedNode.setWidth(newW); }
                    if(newH >= minSize)
                    { selectedNode.setY(localY); selectedNode.setHeight(newH); }
                }
                else if(handle == HandleType.NE)
                {
                    double newW = localX - selectedNode.getX();
                    double newH = (selectedNode.getY() + selectedNode.getHeight()) - localY;
                    if(newW >= minSize) selectedNode.setWidth(newW);
                    if(newH >= minSize)
                    { selectedNode.setY(localY); selectedNode.setHeight(newH); }
                }
                else if(handle == HandleType.SW)
                {
                    double newW = (selectedNode.getX() + selectedNode.getWidth()) - localX;
                    double newH = localY - selectedNode.getY();
                    if(newW >= minSize)
                    { selectedNode.setX(localX); selectedNode.setWidth(newW); }
                    if(newH >= minSize) selectedNode.setHeight(newH);
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
    public void onMouseReleased(MouseEvent e)
    {

        if(handle == HandleType.SELECT_REGION)
        {
            double rx = Math.min(startClickX, e.getX());
            double ry = Math.min(startClickY, e.getY());
            double rw = Math.abs(e.getX() - startClickX);
            double rh = Math.abs(e.getY() - startClickY);

            if(rw > 5 && rh > 5)
            {
                FlowNode lastSelected = null;

                for(FlowNode n : model.getNodes())
                {
                    if(n.getX() < rx + rw && n.getX() + n.getWidth() > rx &&
                            n.getY() < ry + rh && n.getY() + n.getHeight() > ry)
                    {
                        n.setSelected(true);
                        lastSelected = n;
                    }
                }

                for(Connection c : model.getConnections())
                {
                    double[] start = getGlobalCoords(c.getSource().getX() + c.getSrcPctX() * c.getSource().getWidth(), c.getSource().getY() + c.getSrcPctY() * c.getSource().getHeight(), c.getSource());
                    double[] end = getGlobalCoords(c.getTarget().getX() + c.getTgtPctX() * c.getTarget().getWidth(), c.getTarget().getY() + c.getTgtPctY() * c.getTarget().getHeight(), c.getTarget());
                    if(start[0] >= rx && start[0] <= rx+rw && start[1] >= ry && start[1] <= ry+rh &&
                            end[0] >= rx && end[0] <= rx+rw && end[1] >= ry && end[1] <= ry+rh)
                    {
                        c.setSelected(true);
                    }
                }

                if(lastSelected != null)
                {
                    view.showNodeProperties(lastSelected);
                }
            }

            view.setSelectionRegion(false, 0, 0, 0, 0);
            view.drawDiagram();
        }

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

                if(tgtAnchorPcts != null) {
                    Connection newConn = new Connection(connectionSourceNode, targetNode, tempSrcPctX, tempSrcPctY, tgtAnchorPcts[0], tgtAnchorPcts[1]);

                    if (view.isOrthogonalActive())
                    {
                        newConn.setLineStyle(Connection.LineStyle.ORTHOGONAL);
                        newConn.setTgtEndpointStyle(Connection.EndPointStyle.HOLLOW_TRIANGLE);
                    } else
                    {
                        newConn.setLineStyle(Connection.LineStyle.SOLID);
                        newConn.setTgtEndpointStyle(Connection.EndPointStyle.ARROW);
                    }

                    model.addConnection(newConn);
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
        if(handle != HandleType.NONE || view.getEditingNode() != null) return;

        FlowNode nodeUnderMouse = model.findNodeAt(e.getX(), e.getY());

        if(nodeUnderMouse == null && currentHoveredNode != null)
        {
            double cx = currentHoveredNode.getX() + currentHoveredNode.getWidth() / 2;
            double cy = currentHoveredNode.getY() + currentHoveredNode.getHeight() / 2;
            double angleRad = Math.toRadians(-currentHoveredNode.getRotation());
            double localX = cx + ((e.getX() - cx) * Math.cos(angleRad) - (e.getY() - cy) * Math.sin(angleRad));
            double localY = cy + ((e.getX() - cx) * Math.sin(angleRad) + (e.getY() - cy) * Math.cos(angleRad));

            if(localX >= currentHoveredNode.getX() - 20 && localX <= currentHoveredNode.getX() + currentHoveredNode.getWidth() + 20 &&
                    localY >= currentHoveredNode.getY() - 20 && localY <= currentHoveredNode.getY() + currentHoveredNode.getHeight() + 20)
            {
                nodeUnderMouse = currentHoveredNode;
            }
        }

        if(nodeUnderMouse != currentHoveredNode)
        {
            currentHoveredNode = nodeUnderMouse;
            hoverTimer.stop();
            if(currentHoveredNode != null) hoverTimer.playFromStart();
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
    }
}
