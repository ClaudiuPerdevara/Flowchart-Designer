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
            if(x>=node.getX() && x<=node.getX()+node.getWidth() && y>=node.getY() && y<=node.getY()+node.getHeight())
            {
                return node;
            }
        }
        return null;
    }
}
