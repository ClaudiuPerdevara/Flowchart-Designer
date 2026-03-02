package com.designer.model;

import java.util.ArrayList;
import java.util.List;

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
}
