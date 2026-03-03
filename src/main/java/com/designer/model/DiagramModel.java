package com.designer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

public class DiagramModel {
    private final List<FlowNode> nodes=new ArrayList<>();

    public void addNode(FlowNode node)
    {
        nodes.add(node);
    }

    public List<FlowNode> getNodes()
    {
        return nodes;
    }

    public FlowNode findNodeAt(double x, double y)
    {
        //parcurg invers ca sa selectez nodul cel mai de sus
        for(int i=nodes.size()-1;i>=0;i--)
        {
            FlowNode node=nodes.get(i);

            double cx= node.getX()+ node.getWidth()/2;
            double cy=node.getY()+node.getHeight()/2;

            double angleRad = Math.toRadians(-node.getRotation());
            double dx = x - cx;
            double dy = y - cy;

            double localX = cx + (dx * Math.cos(angleRad) - dy * Math.sin(angleRad));
            double localY = cy + (dx * Math.sin(angleRad) + dy * Math.cos(angleRad));

            if (localX >= node.getX() && localX <= node.getX() + node.getWidth() &&
                    localY >= node.getY() && localY <= node.getY() + node.getHeight())
            {
                return node;
            }
        }
        return null;
    }
}
