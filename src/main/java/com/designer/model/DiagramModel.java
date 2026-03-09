package com.designer.model;

import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

public class DiagramModel {
    private final List<FlowNode> nodes=new ArrayList<>();
    private java.util.List<Connection> connections=new java.util.ArrayList<>();

    public void addNode(FlowNode node)
    {
        nodes.add(node);
    }

    public List<FlowNode> getNodes()
    {
        return nodes;
    }

    public FlowNode findNodeAt(double x, double y) {

        for (int i = nodes.size() - 1; i >= 0; i--)
        {
            FlowNode n = nodes.get(i);

            double cx = n.getX() + n.getWidth() / 2;
            double cy = n.getY() + n.getHeight() / 2;

            double angleRad = Math.toRadians(-n.getRotation());
            double localX = cx + ((x - cx) * Math.cos(angleRad) - (y - cy) * Math.sin(angleRad));
            double localY = cy + ((x - cx) * Math.sin(angleRad) + (y - cy) * Math.cos(angleRad));

            if (localX >= n.getX() && localX <= n.getX() + n.getWidth() &&
                    localY >= n.getY() && localY <= n.getY() + n.getHeight()) {
                return n;
            }
        }
        return null;
    }

    public void addConnection(Connection c){
        this.connections.add(c);
    }
    public java.util.List<Connection> getConnections() {
        return this.connections;
    }

    public void removeNode(FlowNode node)
    {
        this.nodes.remove(node);
        this.connections.removeIf(c->c.getSource()==node || c.getTarget()==node);
    }
    public void removeConnection(Connection c)
    {
        this.connections.remove(c);
    }
}
